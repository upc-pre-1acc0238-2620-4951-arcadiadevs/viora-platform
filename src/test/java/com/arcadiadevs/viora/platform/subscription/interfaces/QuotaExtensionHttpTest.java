package com.arcadiadevs.viora.platform.subscription.interfaces;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccountSnapshot;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import com.arcadiadevs.viora.platform.subscription.application.ports.PaymentGateway;
import com.arcadiadevs.viora.platform.subscription.application.ports.ProducerProfilePort;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.QuotaExtensionCodeRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Plan extensions over real HTTP, JWT authentication and the database, with a simulated payment provider. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(QuotaExtensionHttpTest.FixedTime.class)
class QuotaExtensionHttpTest {
    private static final Instant NOW = Instant.parse("2028-03-01T12:00:00Z");
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final SubscriptionPricingPolicy pricing = new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18"));
    @LocalServerPort private int port;
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private CooperativeLicenseRepository licenses;
    @Autowired private InvitationCodeRepository invitationCodes;
    @Autowired private QuotaExtensionCodeRepository extensionCodes;
    @Autowired private UserAccountRepository accounts;
    @Autowired private TokenService tokens;
    @Autowired private CheckoutService payments;
    @MockitoBean private PaymentGateway gateway;
    @MockitoBean private ProducerProfilePort profiles;
    private ProducerId owner;
    private String token;

    @TestConfiguration
    static class FixedTime {
        @Bean
        @Primary
        Clock extensionClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void createVerifiedAccount() {
        var userId = new UserId();
        var email = new EmailAddress("extension-" + userId.userId() + "@example.test");
        accounts.save(UserAccount.reconstitute(new UserAccountSnapshot(userId, email,
                new PasswordHash("unused-test-hash"), PasswordHistory.empty(), 3L,
                UserRole.ROLE_PRODUCER, UserAccountStatus.ACTIVE, null, null, null, NOW, NOW)));
        when(gateway.configured()).thenReturn(true);
        when(gateway.create(any(), any())).thenAnswer(invocation -> new PaymentGateway.Checkout(
                "fixture-" + UUID.randomUUID(), "https://sandbox.mercadopago.com.pe/checkout/test"));
        when(gateway.recover(any())).thenReturn(Optional.empty());
        when(profiles.isReady(anyString())).thenReturn(true);
        owner = new ProducerId(userId.userId());
        token = tokens.generateAccessToken(userId.userId(), email.value(), "ROLE_PRODUCER", 3L);
    }

    @Test
    void paidExtensionAddsHectaresOnlyWhenThePaymentIsApproved() throws Exception {
        var plan = activeIndividual(NOW.minus(Duration.ofDays(60)));
        var base = "/api/v1/subscriptions/" + id(plan);

        var terms = send("GET", base + "/extension-terms", null, null);
        assertEquals(200, terms.statusCode(), terms.body());
        assertEquals("PAY", JsonPath.read(terms.body(), "$.action"));
        assertEquals(10, (int) JsonPath.read(terms.body(), "$.billableMonths"));

        var created = send("POST", base + "/extensions", "{\"addedHa\":3}", "extend-1");
        assertEquals(201, created.statusCode(), created.body());
        assertEquals("PENDING_PAYMENT", JsonPath.read(created.body(), "$.status"));
        assertEquals(1650.0, ((Number) JsonPath.read(created.body(), "$.totalAmount")).doubleValue());
        assertEquals(12.0, ((Number) JsonPath.read(created.body(), "$.quotaHa")).doubleValue());
        String extensionId = JsonPath.read(created.body(), "$.id");
        var replay = send("POST", base + "/extensions", "{\"addedHa\":3}", "extend-1");
        assertEquals(200, replay.statusCode());
        assertEquals(extensionId, JsonPath.read(replay.body(), "$.id"));

        var checkout = send("POST", base + "/extensions/" + extensionId + "/checkouts", null, "pay-1");
        assertEquals(201, checkout.statusCode(), checkout.body());
        var intentId = UUID.fromString(JsonPath.read(checkout.body(), "$.intentId"));
        var paymentId = Long.toString(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        when(gateway.verify(paymentId)).thenReturn(new PaymentGateway.VerifiedPayment(paymentId, intentId, "approved",
                new Money(new BigDecimal("1650.00")), "PEN", NOW, NOW));
        assertTrue(payments.confirm(paymentId).isSuccess());
        assertTrue(payments.confirm(paymentId).isSuccess());

        var applied = send("GET", base + "/extensions/" + extensionId, null, null);
        assertEquals("APPLIED", JsonPath.read(applied.body(), "$.status"));
        assertEquals("APPROVED", JsonPath.read(applied.body(), "$.paymentStatus"));
        var grown = subscriptions.findById(plan.snapshot().id()).orElseThrow().snapshot();
        assertEquals(new BigDecimal("15.00"), grown.quota().hectares());
        assertEquals(plan.snapshot().period().endsAt(), grown.period().endsAt());
    }

    @Test
    void nearTheEndTheProducerRenewsInsteadAndTheNewYearStartsWhenTheCurrentEnds() throws Exception {
        var plan = activeIndividual(NOW.minus(Duration.ofDays(350)));
        var base = "/api/v1/subscriptions/" + id(plan);
        assertEquals("RENEW", JsonPath.read(send("GET", base + "/extension-terms", null, null).body(), "$.action"));
        var refused = send("POST", base + "/extensions", "{\"addedHa\":3}", "late");
        assertEquals(409, refused.statusCode());
        assertEquals("subscription.extension.renewal_window", JsonPath.read(refused.body(), "$.reason"));

        var renewal = send("POST", "/api/v1/subscriptions", "{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":15}", "renew");
        assertEquals(201, renewal.statusCode(), renewal.body());
        String renewalId = JsonPath.read(renewal.body(), "$.id");
        var checkout = send("POST", "/api/v1/subscriptions/" + renewalId + "/checkouts", null, "renew-pay");
        var intentId = UUID.fromString(JsonPath.read(checkout.body(), "$.intentId"));
        var paymentId = Long.toString(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        var total = pricing.quote(new IndividualHectaresQuota(new BigDecimal("15"))).total();
        when(gateway.verify(paymentId)).thenReturn(new PaymentGateway.VerifiedPayment(paymentId, intentId, "approved",
                total, "PEN", NOW, NOW));
        assertTrue(payments.confirm(paymentId).isSuccess());
        var renewed = subscriptions.findById(new SubscriptionId(renewalId)).orElseThrow().snapshot();
        assertEquals(plan.snapshot().period().endsAt(), renewed.period().startsAt());
        var renewalDetails = send("GET", "/api/v1/subscriptions/" + renewalId, null, null);
        assertEquals("ACTIVE", JsonPath.read(renewalDetails.body(), "$.status"));
        assertFalse((Boolean) JsonPath.read(renewalDetails.body(), "$.entitlementActive"));

        var again = send("POST", "/api/v1/subscriptions", "{\"planCode\":\"PRODUCER_ANNUAL\",\"requestedQuotaHa\":1}", "renew-twice");
        assertEquals(409, again.statusCode());
    }

    @Test
    void extensionCodeAddsHectaresOncePerProducerOfItsLicense() throws Exception {
        var license = CooperativeLicense.grant(new CooperativeId(), "Coop. Pruebas", 10,
                new HectaresQuota(new BigDecimal("100")),
                new SubscriptionPeriod(NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(300))));
        var invitation = license.issueCode(new CooperativeCode("COOP-" + randomSuffix()).fingerprint(),
                new HectaresQuota(new BigDecimal("12")), NOW.plus(Duration.ofDays(5)));
        var plan = Subscription.activateFromCode(license.redeem(invitation, owner, NOW), NOW);
        var code = "AMPL-" + randomSuffix();
        var extensionCode = license.issueExtensionCode(new ExtensionCode(code).fingerprint(),
                new HectaresQuota(new BigDecimal("5")), NOW.plus(Duration.ofDays(5)));
        licenses.save(license);
        invitationCodes.save(invitation);
        extensionCodes.save(extensionCode);
        subscriptions.save(plan);

        var redeemed = send("POST", "/api/v1/quota-extension-redemptions", "{\"code\":\"" + code.toLowerCase() + "\"}", null);
        assertEquals(201, redeemed.statusCode(), redeemed.body());
        assertEquals("CODE", JsonPath.read(redeemed.body(), "$.source"));
        assertEquals("APPLIED", JsonPath.read(redeemed.body(), "$.status"));
        assertEquals(17.0, ((Number) JsonPath.read(redeemed.body(), "$.quotaHa")).doubleValue());

        var twice = send("POST", "/api/v1/quota-extension-redemptions", "{\"code\":\"" + code + "\"}", null);
        assertEquals(400, twice.statusCode());
        assertEquals("subscription.extension_code.not_redeemable", JsonPath.read(twice.body(), "$.detail"));
        var unknown = send("POST", "/api/v1/quota-extension-redemptions", "{\"code\":\"AMPL-" + randomSuffix() + "\"}", null);
        assertEquals(400, unknown.statusCode());
        assertEquals(new BigDecimal("17.00"), subscriptions.findById(plan.snapshot().id()).orElseThrow().snapshot().quota().hectares());
        assertEquals("CODE", JsonPath.read(send("GET", "/api/v1/subscriptions/" + id(plan) + "/extension-terms", null, null).body(), "$.action"));
    }

    private Subscription activeIndividual(Instant approvedAt) {
        var quota = new IndividualHectaresQuota(new BigDecimal("12"));
        var price = pricing.quote(quota);
        var plan = Subscription.createIndividual(owner, "PRODUCER_ANNUAL", quota, price, approvedAt.minusSeconds(10));
        plan.activateIndividual(Long.toString(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE)), price.total(), "PEN", approvedAt);
        return subscriptions.save(plan);
    }

    private static String randomSuffix() {
        var letters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        var random = ThreadLocalRandom.current();
        var suffix = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            suffix.append(letters.charAt(random.nextInt(letters.length())));
        }
        return suffix.toString();
    }

    private String id(Subscription subscription) {
        return subscription.snapshot().id().subscriptionId();
    }

    private HttpResponse<String> send(String method, String path, String body, String key) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}

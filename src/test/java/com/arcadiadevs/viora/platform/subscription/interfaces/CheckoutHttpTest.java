package com.arcadiadevs.viora.platform.subscription.interfaces;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.arcadiadevs.viora.platform.subscription.application.ports.PaymentGateway;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.PaymentAttemptRepository;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import com.jayway.jsonpath.JsonPath;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccountSnapshot;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP and database coverage with actual JWT authentication and a simulated external payment port. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "viora.security.demo.enabled=true")
@Import(CheckoutHttpTest.FixedTime.class)
class CheckoutHttpTest {
    private static final Instant NOW = Instant.parse("2028-03-01T12:00:00Z");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    @LocalServerPort private int port;
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private UserAccountRepository accounts;
    @Autowired private TokenService tokens;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PaymentAttemptRepository attempts;
    @Autowired private com.arcadiadevs.viora.platform.subscription.application.ports.SubscriptionUnitOfWork work;
    @MockitoBean private PaymentGateway gateway;
    private ProducerId owner;
    private String token;

    @TestConfiguration
    static class FixedTime {
        @Bean
        @Primary
        Clock currentSubscriptionClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void createVerifiedAccount() {
        var userId = new UserId();
        var email = new EmailAddress("current-" + userId.userId() + "@example.test");
        var account = UserAccount.reconstitute(new UserAccountSnapshot(userId, email,
                new PasswordHash("unused-test-hash"), PasswordHistory.empty(), 3L,
                UserRole.ROLE_PRODUCER, UserAccountStatus.ACTIVE, null, null, null, NOW, NOW));
        accounts.save(account);
        when(gateway.configured()).thenReturn(true);
        when(gateway.create(any(), any())).thenAnswer(invocation -> new PaymentGateway.Checkout(
                "fixture-" + UUID.randomUUID(), "https://sandbox.mercadopago.com.pe/checkout/test"));
        when(gateway.recover(any())).thenReturn(Optional.empty());
        owner = new ProducerId(userId.userId());
        token = tokens.generateAccessToken(userId.userId(), email.value(), "ROLE_PRODUCER", 3L);
    }

    @Test
    void reusesOnePreferenceAndSendsStoredTermsInsteadOfClientAmounts() throws Exception {
        var contract = pending(owner);
        var first = post(id(contract), token, "one");
        assertEquals(201, first.statusCode(), first.body());
        var repeat = post(id(contract), token, "one");
        var anotherKey = post(id(contract), token, "two");
        assertEquals(201, repeat.statusCode());
        assertEquals(201, anotherKey.statusCode());
        assertEquals(JsonPath.<String>read(first.body(), "$.intentId"), JsonPath.<String>read(anotherKey.body(), "$.intentId"));
        assertEquals(NOW.plus(Duration.ofHours(24)).toString(), JsonPath.read(first.body(), "$.expiresAt"));
        var price = ArgumentCaptor.forClass(SubscriptionPrice.class);
        verify(gateway, times(1)).create(any(), price.capture());
        assertEquals(contract.snapshot().price(), price.getValue());
        var reloaded = subscriptions.findById(contract.snapshot().id()).orElseThrow();
        assertEquals(SubscriptionStatus.PENDING_PAYMENT, reloaded.snapshot().status());
        assertNull(reloaded.snapshot().period());
        assertFalse(reloaded.hasActiveEntitlement(NOW));
    }

    @Test
    void foreignAndMissingContractsNeverContactTheProvider() throws Exception {
        var foreign = pending(new ProducerId(UUID.randomUUID().toString()));
        assertEquals(404, post(id(foreign), token, "foreign").statusCode());
        assertEquals(404, post(UUID.randomUUID().toString(), token, "missing").statusCode());
        verifyNoInteractions(gateway);
    }

    @Test
    void missingSessionAndInvalidTokenNeverContactTheProvider() throws Exception {
        var contract = pending(owner);
        assertEquals(401, post(id(contract), null, "anonymous").statusCode());
        assertEquals(401, post(id(contract), "invalid-jwt", "invalid").statusCode());
        verifyNoInteractions(gateway);
    }

    @Test
    void invalidIdentifierAndMissingOrBlankKeysDoNotCreateAnAttempt() throws Exception {
        var contract = pending(owner);
        assertEquals(400, post("not-a-uuid", token, "invalid").statusCode());
        assertEquals(400, post(id(contract), token, null).statusCode());
        var blank = post(id(contract), token, " ").statusCode();
        assertTrue(blank >= 400 && blank < 500);
        assertTrue(attempts.latest(UUID.fromString(id(contract))).isEmpty());
        verifyNoInteractions(gateway);
    }

    @Test
    void disabledProviderReturnsUnavailableWithoutCreatingAnAttempt() throws Exception {
        var contract = pending(owner);
        when(gateway.configured()).thenReturn(false);
        assertEquals(503, post(id(contract), token, "disabled").statusCode());
        assertTrue(attempts.latest(UUID.fromString(id(contract))).isEmpty());
        verify(gateway, never()).create(any(), any());
    }

    @Test
    void activeAndSponsoredContractsCannotStartAnIndividualCheckout() throws Exception {
        var active = pending(owner);
        active.activateIndividual("http-" + UUID.randomUUID(), active.snapshot().price().total(), "PEN", NOW.minusSeconds(1));
        active = subscriptions.save(active);
        assertEquals(409, post(id(active), token, "active").statusCode());
        var cooperative = sponsored(NOW.minusSeconds(10), NOW.plusSeconds(600));
        assertEquals(409, post(id(cooperative), token, "sponsored").statusCode());
        verifyNoInteractions(gateway);
    }

    @Test
    void lostProviderResponseRecoversTheDurableAttemptWithoutAnotherCreate() throws Exception {
        var contract = pending(owner);
        when(gateway.create(any(), any())).thenThrow(new PaymentGateway.Unavailable());
        assertEquals(503, post(id(contract), token, "recover").statusCode());
        when(gateway.recover(any())).thenReturn(Optional.of(new PaymentGateway.Checkout(
                "recovered-" + UUID.randomUUID(), "https://sandbox.mercadopago.com.pe/checkout/test")));
        assertEquals(201, post(id(contract), token, "recover").statusCode());
        verify(gateway, times(1)).create(any(), any());
        verify(gateway, times(1)).recover(any());
        assertFalse(subscriptions.findById(contract.snapshot().id()).orElseThrow().hasActiveEntitlement(NOW));
    }

    @Test
    void concurrentRequestsNeverCreateTwoRemotePreferences() throws Exception {
        var contract = pending(owner);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(gateway.create(any(), any())).thenAnswer(invocation -> {
            entered.countDown();
            if (!release.await(15, TimeUnit.SECONDS)) {
                throw new PaymentGateway.Unavailable();
            }
            return new PaymentGateway.Checkout("concurrent-" + UUID.randomUUID(), "https://sandbox.mercadopago.com.pe/checkout/test");
        });
        try (var pool = Executors.newSingleThreadExecutor()) {
            var first = pool.submit(() -> post(id(contract), token, "first"));
            try {
                assertTrue(entered.await(10, TimeUnit.SECONDS));
                assertEquals(503, post(id(contract), token, "concurrent").statusCode());
            } finally {
                release.countDown();
            }
            assertEquals(201, first.get(20, TimeUnit.SECONDS).statusCode());
        }
        assertEquals(201, post(id(contract), token, "after").statusCode());
        verify(gateway, times(1)).create(any(), any());
    }

    @Test
    void persistedPendingPaymentBlocksReplacementAfterExpiryAndEmptyProviderSearch() throws Exception {
        var quota = new IndividualHectaresQuota(new BigDecimal("0.5"));
        var price = new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(quota);
        var contract = subscriptions.save(Subscription.createIndividual(owner, "PRODUCER_ANNUAL", quota,
                price, NOW.minus(Duration.ofDays(2))));
        var attempt = new PaymentAttempt(UUID.randomUUID(), UUID.fromString(id(contract)), owner.producerId(),
                "OPEN", "observed-" + UUID.randomUUID(), "https://sandbox.mercadopago.com.pe/checkout/test",
                NOW.minus(Duration.ofHours(25)), NOW.minus(Duration.ofHours(1)))
                .withPaymentState("123", "pending");
        attempts.save(attempt);
        when(gateway.findPayments(attempt.id())).thenReturn(java.util.List.of());
        assertEquals(409, post(id(contract), token, "replacement").statusCode());
        var reloaded = attempts.find(attempt.id()).orElseThrow();
        assertEquals("PENDING", reloaded.status());
        assertEquals("pending", reloaded.paymentStates().get("123"));
        verify(gateway, never()).create(any(), any());
    }

    @Test
    void expiredCheckoutReceiptHasAStableRecoveryReasonAndReplacementRemainsServerControlled() throws Exception {
        var contract = pending(owner);
        var attempt = attempts.save(new PaymentAttempt(UUID.randomUUID(), UUID.fromString(id(contract)), owner.producerId(),
                "EXPIRED", "expired-preference", "https://sandbox.mercadopago.com.pe/checkout/test",
                NOW.minus(Duration.ofHours(25)), NOW.minus(Duration.ofHours(1))));
        work.exclusive(owner.producerId(), () -> {
            work.recordOperation(owner.producerId(), "expired-receipt", "CREATE_CHECKOUT", id(contract), attempt.id());
            return com.arcadiadevs.viora.platform.shared.application.result.Result.success(true);
        });
        var expired = post(id(contract), token, "expired-receipt");
        assertEquals(409, expired.statusCode(), expired.body());
        assertEquals("SUBSCRIPTION_CONFLICT", JsonPath.read(expired.body(), "$.code"));
        assertEquals("subscription.checkout.expired", JsonPath.read(expired.body(), "$.reason"));
        verify(gateway, never()).create(any(), any());
        assertEquals(201, post(id(contract), token, "replacement-receipt").statusCode());
        verify(gateway, times(1)).create(any(), any());
        assertFalse(subscriptions.findById(contract.snapshot().id()).orElseThrow().hasActiveEntitlement(NOW));
    }
    private String id(Subscription subscription) {
        return subscription.snapshot().id().subscriptionId();
    }

    private Subscription pending(ProducerId producer) {
        var quota = new IndividualHectaresQuota(new BigDecimal("0.5"));
        var price = new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(quota);
        return subscriptions.save(Subscription.createIndividual(producer, "PRODUCER_ANNUAL", quota, price, NOW.minusSeconds(60)));
    }

    private Subscription sponsored(Instant starts, Instant ends) {
        return subscriptions.save(Subscription.reconstitute(new SubscriptionSnapshot(new SubscriptionId(), owner,
                PlanMode.PLAN_COOPERATIVO_PATROCINADO, SubscriptionStatus.ACTIVE,
                new HectaresQuota(new BigDecimal("0.25")), new SubscriptionPeriod(starts, ends),
                new CooperativeId(UUID.randomUUID().toString()), new CooperativeLicenseId(UUID.randomUUID().toString()),
                new InvitationCodeId(UUID.randomUUID().toString()), 0L)));
    }

    private HttpResponse<String> post(String identifier, String bearer, String key) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/subscriptions/" + identifier + "/checkouts"))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"totalAmount\":1,\"currency\":\"USD\"}"));
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}

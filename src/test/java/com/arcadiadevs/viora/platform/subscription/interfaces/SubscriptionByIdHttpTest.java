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

/** Real HTTP coverage of owned identifier lookup through JWT validation and database persistence. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SubscriptionByIdHttpTest.FixedTime.class)
class SubscriptionByIdHttpTest {
    private static final Instant NOW = Instant.parse("2028-03-01T12:00:00Z");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    @LocalServerPort private int port;
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private com.arcadiadevs.viora.platform.subscription.domain.repositories.PaymentAttemptRepository attempts;
    @Autowired private UserAccountRepository accounts;
    @Autowired private TokenService tokens;
    @Autowired private JdbcTemplate jdbc;
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
        owner = new ProducerId(userId.userId());
        token = tokens.generateAccessToken(userId.userId(), email.value(), "ROLE_PRODUCER", 3L);
    }

    @Test
    void readsTheRequestedPendingContractEvenWhenAnotherOneIsCurrent() throws Exception {
        var requested = pending(owner);
        sponsored(NOW.minusSeconds(10), NOW.plusSeconds(600));
        var response = get(id(requested), token);
        assertContract(response, requested, false);
        assertEquals("PENDING_PAYMENT", JsonPath.read(response.body(), "$.status"));
        assertEquals(330.0, JsonPath.<Double>read(response.body(), "$.totalAmount"));
        assertNull(JsonPath.read(response.body(), "$.startsAt"));
        assertNull(JsonPath.read(response.body(), "$.endsAt"));
    }

    @Test
    void currentQueryExposesPendingPaymentWithoutGrantingAccess() throws Exception {
        var contract = pending(owner);
        var attempt = new PaymentAttempt(UUID.randomUUID(), UUID.fromString(id(contract)), owner.producerId(),
                "OPEN", "pending-preference", "https://sandbox.mercadopago.com.pe/checkout", NOW, NOW.plusSeconds(3600));
        attempts.save(attempt.withPaymentState("987654322", "pending"));
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/subscriptions"))
                .header("Authorization", "Bearer " + token).GET().build();
        var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("PENDING", JsonPath.read(response.body(), "$.paymentStatus"));
        assertEquals(false, JsonPath.read(response.body(), "$.entitlementActive"));
    }

    @Test
    void ignoresPaymentEvidenceOwnedByAnotherProducer() throws Exception {
        var contract = pending(owner);
        var attempt = new PaymentAttempt(UUID.randomUUID(), UUID.fromString(id(contract)), UUID.randomUUID().toString(),
                "OPEN", "foreign-preference", "https://sandbox.mercadopago.com.pe/checkout", NOW, NOW.plusSeconds(3600));
        attempts.save(attempt.withPaymentState("987654323", "rejected"));
        var response = get(id(contract), token);
        assertEquals(200, response.statusCode());
        assertNull(JsonPath.read(response.body(), "$.paymentStatus"));
        assertEquals(false, JsonPath.read(response.body(), "$.entitlementActive"));
    }

    @Test
    void exposesVerifiedRejectionWithoutGrantingAccess() throws Exception {
        var contract = pending(owner);
        var attempt = new PaymentAttempt(UUID.randomUUID(), UUID.fromString(id(contract)), owner.producerId(),
                "OPEN", "test-preference", "https://sandbox.mercadopago.com.pe/checkout", NOW, NOW.plusSeconds(3600));
        attempts.save(attempt.withPaymentState("987654321", "rejected"));
        var response = get(id(contract), token);
        assertContract(response, contract, false);
        assertEquals("REJECTED", JsonPath.read(response.body(), "$.paymentStatus"));
        assertEquals("PENDING_PAYMENT", JsonPath.read(response.body(), "$.status"));
    }

    @Test
    void readsOwnedApprovedIndividualTerms() throws Exception {
        var active = pending(owner);
        active.activateIndividual("by-id-" + UUID.randomUUID(), active.snapshot().price().total(), "PEN", NOW.minusSeconds(1));
        active = subscriptions.save(active);
        var response = get(id(active), token);
        assertContract(response, active, true);
        assertEquals("ACTIVE", JsonPath.read(response.body(), "$.status"));
        assertEquals("PLAN_PRODUCTOR_INDIVIDUAL", JsonPath.read(response.body(), "$.mode"));
    }

    @Test
    void preservesOwnedSponsoredModeQuotaAndOptionalAmounts() throws Exception {
        var active = sponsored(NOW.minusSeconds(10), NOW.plusSeconds(600));
        var response = get(id(active), token);
        assertContract(response, active, true);
        assertEquals("PLAN_COOPERATIVO_PATROCINADO", JsonPath.read(response.body(), "$.mode"));
        assertEquals(0.25, JsonPath.<Double>read(response.body(), "$.quotaHa"));
        assertEquals(active.snapshot().cooperativeId().cooperativeId(), JsonPath.read(response.body(), "$.cooperativeId"));
        assertNull(JsonPath.read(response.body(), "$.totalAmount"));
    }

    @Test
    void projectsExpiredAccessWithoutUpdatingTheStoredContract() throws Exception {
        var expired = sponsored(NOW.minusSeconds(600), NOW);
        var before = subscriptions.findById(expired.snapshot().id()).orElseThrow().snapshot();
        var response = get(id(expired), token);
        assertContract(response, expired, false);
        assertEquals("EXPIRED", JsonPath.read(response.body(), "$.status"));
        assertEquals(before, subscriptions.findById(expired.snapshot().id()).orElseThrow().snapshot());
    }

    @Test
    void foreignAndMissingIdentifiersHaveTheSamePublicErrorCategory() throws Exception {
        var foreign = pending(new ProducerId(UUID.randomUUID().toString()));
        var hidden = get(id(foreign), token);
        var missing = get(UUID.randomUUID().toString(), token);
        assertEquals(404, hidden.statusCode());
        assertEquals(404, missing.statusCode());
        for (var field : new String[] {"type", "title", "status", "code"}) {
            assertEquals(JsonPath.<Object>read(missing.body(), "$." + field),
                    JsonPath.<Object>read(hidden.body(), "$." + field));
        }
        assertFalse(hidden.body().contains("PRODUCER_ANNUAL"));
    }

    @Test
    void foreignPersistenceDataIsNeverReconstructedBeforeOwnershipIsChecked() throws Exception {
        var foreign = pending(new ProducerId(UUID.randomUUID().toString()));
        var rowId = UUID.fromString(id(foreign));
        jdbc.update("update subscriptions set base_amount = ? where id = ?", BigDecimal.ONE, rowId);
        try {
            // A foreign row with inconsistent payment terms must stay invisible, not cause an assembler error.
            assertEquals(404, get(id(foreign), token).statusCode());
        } finally {
            jdbc.update("update subscriptions set base_amount = ? where id = ?", foreign.snapshot().price().base().amount(), rowId);
        }
    }

    @Test
    void malformedIdentifierReturnsBadRequest() throws Exception {
        var response = get("not-a-uuid", token);
        assertEquals(400, response.statusCode());
        assertEquals(400, JsonPath.<Integer>read(response.body(), "$.status"));
    }

    @Test
    void validMissingIdentifierReturnsNotFound() throws Exception {
        assertEquals(404, get(UUID.randomUUID().toString(), token).statusCode());
    }

    @Test
    void rejectsAnonymousEvenWithDemoModeEnabled() throws Exception {
        assertEquals(401, get(id(pending(owner)), null).statusCode());
    }

    @Test
    void rejectsMalformedBearerToken() throws Exception {
        assertEquals(401, get(id(pending(owner)), "invalid-jwt").statusCode());
    }

    @Test
    void rejectsASignedTokenForAnUnknownAccount() throws Exception {
        var unknown = tokens.generateAccessToken(UUID.randomUUID().toString(), "unknown@example.test", "ROLE_PRODUCER", 0L);
        assertEquals(401, get(id(pending(owner)), unknown).statusCode());
    }

    @Test
    void rejectsATokenWithAnOutdatedCredentialsVersion() throws Exception {
        var outdated = tokens.generateAccessToken(owner.producerId(), "old@example.test", "ROLE_PRODUCER", 2L);
        assertEquals(401, get(id(pending(owner)), outdated).statusCode());
    }

    @Test
    void clientSuppliedOwnerCannotAuthorizeAForeignIdentifier() throws Exception {
        var foreign = pending(new ProducerId(UUID.randomUUID().toString()));
        assertEquals(404, get(id(foreign) + "?producerId=" + foreign.snapshot().producerId().producerId(), token).statusCode());
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

    private HttpResponse<String> get(String identifier, String bearer) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/subscriptions/" + identifier))
                .timeout(Duration.ofSeconds(20)).GET();
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertContract(HttpResponse<String> response, Subscription expected, boolean active) {
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(expected.snapshot().id().subscriptionId(), JsonPath.read(response.body(), "$.id"));
        assertEquals(active, JsonPath.<Boolean>read(response.body(), "$.entitlementActive"));
    }
}

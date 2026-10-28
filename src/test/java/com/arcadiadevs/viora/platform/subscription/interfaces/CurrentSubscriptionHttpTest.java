package com.arcadiadevs.viora.platform.subscription.interfaces;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
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

/** Real HTTP, JWT validation and database coverage without mocking the authentication or read path. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(CurrentSubscriptionHttpTest.FixedTime.class)
class CurrentSubscriptionHttpTest {
    private static final Instant NOW = Instant.parse("2028-03-01T12:00:00Z");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    @LocalServerPort private int port;
    @Autowired private SubscriptionRepository subscriptions;
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
    void returnsNoContentWithoutLeakingAnotherProducersContract() throws Exception {
        pending(new ProducerId(UUID.randomUUID().toString()));
        var response = get("", token);
        assertEquals(204, response.statusCode());
        assertTrue(response.body().isEmpty());
    }

    @Test
    void returnsOwnPendingTermsAndIgnoresClientSuppliedOwner() throws Exception {
        var own = pending(owner);
        var foreign = pending(new ProducerId(UUID.randomUUID().toString()));
        var response = get("?producerId=" + foreign.snapshot().producerId().producerId(), token);
        assertContract(response, own, false);
        assertEquals("PENDING_PAYMENT", JsonPath.read(response.body(), "$.status"));
        assertEquals("PEN", JsonPath.read(response.body(), "$.currency"));
        assertEquals(330.0, JsonPath.<Double>read(response.body(), "$.totalAmount"));
        assertNull(JsonPath.read(response.body(), "$.startsAt"));
        assertNull(JsonPath.read(response.body(), "$.endsAt"));
    }

    @Test
    void preservesSponsoredAccessDespiteANewerPendingPurchase() throws Exception {
        var sponsored = sponsored(NOW.minusSeconds(60), NOW.plusSeconds(600));
        pending(owner);
        var response = get("", token);
        assertContract(response, sponsored, true);
        assertEquals("PLAN_COOPERATIVO_PATROCINADO", JsonPath.read(response.body(), "$.mode"));
        assertEquals(0.25, JsonPath.<Double>read(response.body(), "$.quotaHa"));
        assertEquals(sponsored.snapshot().cooperativeId().cooperativeId(),
                JsonPath.read(response.body(), "$.cooperativeId"));
        assertNull(JsonPath.read(response.body(), "$.totalAmount"));
    }

    @Test
    void prefersActiveIndividualAccessOverNewerExpiredRows() throws Exception {
        var active = pending(owner);
        active.activateIndividual("http-" + UUID.randomUUID(), active.snapshot().price().total(), "PEN", NOW.minusSeconds(1));
        active = subscriptions.save(active);
        var expired = sponsored(NOW.minusSeconds(600), NOW.minusSeconds(1));
        registeredAt(active, NOW.minusSeconds(300));
        registeredAt(expired, NOW);
        var response = get("", token);
        assertContract(response, active, true);
        assertEquals("ACTIVE", JsonPath.read(response.body(), "$.status"));
    }

    @Test
    void fallsBackToTheLatestRegisteredContractWhenNoAccessIsActive() throws Exception {
        var older = sponsored(NOW.minusSeconds(600), NOW.minusSeconds(1));
        var latest = pending(owner);
        registeredAt(older, NOW.minusSeconds(60));
        registeredAt(latest, NOW);
        assertContract(get("", token), latest, false);
    }

    @Test
    void endIsExclusiveAndReadingDoesNotModifyStoredState() throws Exception {
        var expired = sponsored(NOW.minusSeconds(600), NOW);
        var before = subscriptions.findById(expired.snapshot().id()).orElseThrow().snapshot();
        var response = get("", token);
        assertContract(response, expired, false);
        assertEquals("EXPIRED", JsonPath.read(response.body(), "$.status"));
        assertEquals(before, subscriptions.findById(expired.snapshot().id()).orElseThrow().snapshot());
    }

    @Test
    void startIsInclusiveAndActiveAccessWinsOverPending() throws Exception {
        var active = sponsored(NOW, NOW.plusSeconds(600));
        pending(owner);
        assertContract(get("", token), active, true);
    }

    @Test
    void rejectsAnonymousEvenWithDemoModeEnabled() throws Exception {
        pending(owner);
        assertEquals(401, get("", null).statusCode());
    }

    @Test
    void rejectsMalformedBearerToken() throws Exception {
        assertEquals(401, get("", "invalid-jwt").statusCode());
    }

    @Test
    void rejectsASignedTokenForAnUnknownAccount() throws Exception {
        var unknown = tokens.generateAccessToken(UUID.randomUUID().toString(), "unknown@example.test", "ROLE_PRODUCER", 0L);
        assertEquals(401, get("", unknown).statusCode());
    }

    @Test
    void rejectsATokenWithAnOutdatedCredentialsVersion() throws Exception {
        var outdated = tokens.generateAccessToken(owner.producerId(), "old@example.test", "ROLE_PRODUCER", 2L);
        assertEquals(401, get("", outdated).statusCode());
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

    private void registeredAt(Subscription subscription, Instant instant) {
        jdbc.update("update subscriptions set created_at = ? where id = ?",
                Timestamp.from(instant), UUID.fromString(subscription.snapshot().id().subscriptionId()));
    }

    private HttpResponse<String> get(String query, String bearer) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/subscriptions" + query))
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

package com.arcadiadevs.viora.platform.subscription.interfaces.rest;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CooperativeCodeRedemptionCommandService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCodeSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemCooperativeCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.InvitationCodePersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionPersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.CooperativeLicensePersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.InvitationCodePersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionPersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.seeding.SubscriptionDemoDataSeeder;
import com.arcadiadevs.viora.platform.subscription.infrastructure.seeding.SubscriptionTestCodesSeeder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cooperative code redemption against the whole application: real producers, real codes, real database.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cooperativecoderedemption;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "viora.subscription.seed.enabled=true",
        "viora.security.demo.enabled=true"
})
@DisplayName("Cooperative code redemption integration")
class CooperativeCodeRedemptionIntegrationTest {

    private static final String URL = "/api/v1/cooperative-code-redemptions";
    private static final String NOT_REDEEMABLE_EN = "Expired or already redeemed code. Ask your cooperative for a new one.";
    private static final String NOT_REDEEMABLE_ES = "Código vencido o ya canjeado. Pide uno nuevo a tu cooperativa.";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired WebApplicationContext context;
    @Autowired UserAccountCommandService userAccountCommandService;
    @Autowired CooperativeCodeRedemptionCommandService redemptionCommandService;
    @Autowired CooperativeLicenseRepository licenseRepository;
    @Autowired InvitationCodeRepository invitationCodeRepository;
    @Autowired CooperativeLicensePersistenceRepository licensePersistenceRepository;
    @Autowired InvitationCodePersistenceRepository codePersistenceRepository;
    @Autowired SubscriptionPersistenceRepository subscriptionPersistenceRepository;

    private MockMvc mvc;
    private String producer;
    private CooperativeLicense license;
    private Instant licenseEnd;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        producer = signUp();
        licenseEnd = Instant.now().plus(Duration.ofDays(120)).truncatedTo(ChronoUnit.SECONDS);
        license = newLicense(new BigDecimal("200"));
        authenticateAs(producer);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("A producer redeems an available code: 201 with the sponsored subscription, and everything is stored")
    void redeemsAnAvailableCode() throws Exception {
        var code = newCode(license, Instant.now().plus(Duration.ofDays(7)), new BigDecimal("12"));
        assertThat(committedArea(license)).isEqualByComparingTo("12");

        redeem(code)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mode").value("PLAN_COOPERATIVO_PATROCINADO"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.quotaHa").value(12.0))
                .andExpect(jsonPath("$.entitlementActive").value(true))
                .andExpect(jsonPath("$.cooperativeId").value(license.snapshot().cooperativeId().cooperativeId()))
                .andExpect(jsonPath("$.cooperativeName").value("Coop. Olivarera Test"))
                .andExpect(jsonPath("$.endsAt").value(licenseEnd.toString()));

        var storedCode = storedCode(code);
        assertThat(storedCode.getStatus()).isEqualTo(InvitationCodeStatus.REDEEMED);
        assertThat(storedCode.getRedeemedBy()).hasToString(producer);
        assertThat(storedCode.getRedeemedAt()).isNotNull();
        assertThat(committedArea(license)).isEqualByComparingTo("12");
        assertThat(subscriptionsOf(producer)).singleElement()
                .satisfies(subscription -> assertThat(subscription.getRedeemedCodeId()).isEqualTo(storedCode.getId()));
    }

    @Test
    @DisplayName("The code is accepted in lowercase and with spaces around it")
    void codeIsNormalized() throws Exception {
        var code = newCode(license, Instant.now().plus(Duration.ofDays(7)));

        redeem("  " + code.toLowerCase() + " ").andExpect(status().isCreated());

        assertThat(storedCode(code).getStatus()).isEqualTo(InvitationCodeStatus.REDEEMED);
    }

    @Test
    @DisplayName("An unknown, expired, revoked or already redeemed code answers the same 400 and changes nothing")
    void codesThatCannotBeRedeemed() throws Exception {
        var expired = newCode(license, Instant.now().minus(Duration.ofMinutes(1)));
        var revoked = newCode(license, Instant.now().plus(Duration.ofDays(7)), InvitationCodeStatus.REVOKED);
        var taken = newCode(license, Instant.now().plus(Duration.ofDays(7)));
        var otherProducer = signUp();
        authenticateAs(otherProducer);
        redeem(taken).andExpect(status().isCreated());
        authenticateAs(producer);
        var areaBefore = committedArea(license);

        for (var code : List.of("COOP-ZZZZ", expired, revoked, taken)) {
            redeem(code)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.detail").value(NOT_REDEEMABLE_EN));
        }
        assertThat(storedCode(expired).getStatus()).isEqualTo(InvitationCodeStatus.AVAILABLE);
        assertThat(storedCode(revoked).getStatus()).isEqualTo(InvitationCodeStatus.REVOKED);
        assertThat(storedCode(taken).getRedeemedBy()).hasToString(otherProducer);
        assertThat(committedArea(license)).isEqualByComparingTo(areaBefore);
        assertThat(subscriptionsOf(producer)).isEmpty();
    }

    @Test
    @DisplayName("Without a session the code is not redeemed, even with the demo producer fallback on")
    void anonymousRequestIsRejected() throws Exception {
        var code = newCode(license, Instant.now().plus(Duration.ofDays(7)));
        SecurityContextHolder.clearContext();

        redeem(code).andExpect(status().isUnauthorized());

        assertThat(storedCode(code).getStatus()).isEqualTo(InvitationCodeStatus.AVAILABLE);
    }

    @Test
    @DisplayName("The refusal is in Spanish for a Spanish speaking producer")
    void refusalIsLocalized() throws Exception {
        mvc.perform(post(URL).header(HttpHeaders.ACCEPT_LANGUAGE, "es")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("COOP-ZZZZ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(NOT_REDEEMABLE_ES));
    }

    @Test
    @DisplayName("The demo seed grants a license whose available code is redeemable and whose closed codes are not")
    void seededCodes() throws Exception {
        redeem(SubscriptionDemoDataSeeder.AVAILABLE_CODES.firstEntry().getKey())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cooperativeName").value("Coop. Olivarera La Yarada"))
                .andExpect(jsonPath("$.quotaHa").value(12.0));

        authenticateAs(signUp());
        redeem(SubscriptionDemoDataSeeder.EXPIRED_CODE).andExpect(status().isBadRequest());
        redeem(SubscriptionDemoDataSeeder.REVOKED_CODE).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("The test codes seed a license of its own whose codes activate the plan size they name")
    void seededTestCodes() throws Exception {
        redeem("COOP-T05A")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cooperativeName").value(SubscriptionTestCodesSeeder.COOPERATIVE_NAME))
                .andExpect(jsonPath("$.quotaHa").value(5.0));

        authenticateAs(signUp());
        redeem("COOP-T05A").andExpect(status().isBadRequest());
        redeem("COOP-T20E").andExpect(status().isCreated()).andExpect(jsonPath("$.quotaHa").value(20.0));
        assertThat(SubscriptionTestCodesSeeder.CODES).hasSize(20).containsKeys("COOP-T02A", "COOP-T12C");
    }

    @Test
    @DisplayName("A producer with an active subscription gets 409 and the second code stays available")
    void producerAlreadySubscribed() throws Exception {
        var first = newCode(license, Instant.now().plus(Duration.ofDays(7)));
        var second = newCode(license, Instant.now().plus(Duration.ofDays(7)));
        redeem(first).andExpect(status().isCreated());

        redeem(second)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("You already have an active subscription"));

        assertThat(storedCode(second).getStatus()).isEqualTo(InvitationCodeStatus.AVAILABLE);
        assertThat(subscriptionsOf(producer)).hasSize(1);
    }

    @Test
    @DisplayName("Hectares sent by the client are ignored: the subscription covers the code's quota")
    void clientHectaresAreIgnored() throws Exception {
        var code = newCode(license, Instant.now().plus(Duration.ofDays(7)), new BigDecimal("8"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"" + code + "\", \"quotaHa\": 150}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quotaHa").value(8.0));

        assertThat(committedArea(license)).isEqualByComparingTo("8");
    }

    @Test
    @DisplayName("A payload without a code, with a blank code or with a malformed code answers 400")
    void invalidPayload() throws Exception {
        var code = newCode(license, Instant.now().plus(Duration.ofDays(7)));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        redeem(" ").andExpect(status().isBadRequest());
        redeem("OLIV-4F9T")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("The code must be COOP- followed by four letters or digits, for example COOP-7K2Q"));

        assertThat(storedCode(code).getStatus()).isEqualTo(InvitationCodeStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Without a session the redemption answers 401")
    void noSession() throws Exception {
        SecurityContextHolder.clearContext();

        redeem(newCode(license, Instant.now().plus(Duration.ofDays(7)))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("After five codes that cannot be redeemed the producer waits, even for a valid code")
    void tooManyFailedAttempts() throws Exception {
        for (var attempt = 0; attempt < 5; attempt++) {
            redeem(randomCode()).andExpect(status().isBadRequest());
        }
        var valid = newCode(license, Instant.now().plus(Duration.ofDays(7)));

        redeem(valid)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.code").value("COOPERATIVE_CODE_REDEMPTION_TOO_MANY_REQUESTS"));

        assertThat(storedCode(valid).getStatus()).isEqualTo(InvitationCodeStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Forty wrong codes sent at the same time: only five are tried, the rest wait")
    void burstOfWrongCodesIsCappedAtFive() throws Exception {
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(40);
        try {
            var futures = new ArrayList<Future<Result<Subscription, ApplicationError>>>();
            for (var attempt = 0; attempt < 40; attempt++) {
                var guess = randomCode();
                Callable<Result<Subscription, ApplicationError>> task = () -> {
                    start.await();
                    return redemptionCommandService.handle(
                            new RedeemCooperativeCodeCommand(producer, guess));
                };
                futures.add(executor.submit(task));
            }
            start.countDown();
            var codes = new ArrayList<String>();
            for (var future : futures) {
                codes.add(future.get().failure().orElseThrow().code());
            }

            assertThat(codes).filteredOn("VALIDATION_ERROR"::equals).hasSize(5);
            assertThat(codes).filteredOn("COOPERATIVE_CODE_REDEMPTION_TOO_MANY_REQUESTS"::equals).hasSize(35);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Refusals that are not a wrong code, such as a malformed code or a 409, do not use up attempts")
    void otherRefusalsDoNotCount() throws Exception {
        for (var attempt = 0; attempt < 6; attempt++) {
            redeem("OLIV-4F9T").andExpect(status().isBadRequest());
        }
        redeem(newCode(license, Instant.now().plus(Duration.ofDays(7)))).andExpect(status().isCreated());
        for (var attempt = 0; attempt < 6; attempt++) {
            redeem(newCode(license, Instant.now().plus(Duration.ofDays(7)))).andExpect(status().isConflict());
        }
    }

    @Test
    @DisplayName("Two producers redeeming the same code at the same time: one gets it, the other gets the 400")
    void concurrentRedemptionsOfTheSameCode() throws Exception {
        var code = newCode(license, Instant.now().plus(Duration.ofDays(7)), new BigDecimal("5"));
        var producers = List.of(signUp(), signUp());
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var futures = new ArrayList<Future<Result<Subscription, ApplicationError>>>();
            for (var each : producers) {
                Callable<Result<Subscription, ApplicationError>> task = () -> {
                    start.await();
                    return redemptionCommandService.handle(
                            new RedeemCooperativeCodeCommand(each, code));
                };
                futures.add(executor.submit(task));
            }
            start.countDown();
            var results = new ArrayList<Result<Subscription, ApplicationError>>();
            for (var future : futures) {
                results.add(future.get());
            }

            assertThat(results).filteredOn(Result::isSuccess).hasSize(1);
            assertThat(results).filteredOn(Result::isFailure).singleElement()
                    .satisfies(failure -> assertThat(failure.failure().orElseThrow().code()).isEqualTo("VALIDATION_ERROR"));
        } finally {
            executor.shutdownNow();
        }

        var storedCode = storedCode(code);
        assertThat(storedCode.getStatus()).isEqualTo(InvitationCodeStatus.REDEEMED);
        assertThat(producers).contains(storedCode.getRedeemedBy().toString());
        assertThat(committedArea(license)).isEqualByComparingTo("5");
        assertThat(subscriptionPersistenceRepository.findAll())
                .filteredOn(subscription -> storedCode.getId().equals(subscription.getRedeemedCodeId()))
                .hasSize(1);
    }

    private ResultActions redeem(String code) throws Exception {
        return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body(code)));
    }

    private static String body(String code) {
        return "{\"code\": \"" + code + "\"}";
    }

    private CooperativeLicense newLicense(BigDecimal areaHa) {
        return licenseRepository.save(CooperativeLicense.grant(new CooperativeId(), "Coop. Olivarera Test", 60,
                new HectaresQuota(areaHa), new SubscriptionPeriod(Instant.now().minus(Duration.ofDays(1)), licenseEnd)));
    }

    private String newCode(CooperativeLicense owner, Instant expiresAt) {
        return newCode(owner, expiresAt, InvitationCodeStatus.AVAILABLE, BigDecimal.ONE);
    }

    private String newCode(CooperativeLicense owner, Instant expiresAt, BigDecimal quotaHa) {
        return newCode(owner, expiresAt, InvitationCodeStatus.AVAILABLE, quotaHa);
    }

    private String newCode(CooperativeLicense owner, Instant expiresAt, InvitationCodeStatus status) {
        return newCode(owner, expiresAt, status, BigDecimal.ONE);
    }

    private String newCode(CooperativeLicense owner, Instant expiresAt, InvitationCodeStatus status,
                           BigDecimal quotaHa) {
        var code = randomCode();
        var stored = licenseRepository.findById(owner.snapshot().id()).orElseThrow();
        var issued = stored.issueCode(new CooperativeCode(code).fingerprint(), new HectaresQuota(quotaHa), expiresAt)
                .snapshot();
        licenseRepository.save(stored);
        invitationCodeRepository.save(InvitationCode.reconstitute(new InvitationCodeSnapshot(issued.id(),
                issued.licenseId(), null, issued.fingerprint(), issued.quota(), status, expiresAt, null, null, 0L)));
        return code;
    }

    private static String randomCode() {
        var alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        var code = new StringBuilder("COOP-");
        for (var index = 0; index < 4; index++) {
            code.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return code.toString();
    }

    private InvitationCodePersistenceEntity storedCode(String code) {
        var fingerprint = new CooperativeCode(code).fingerprint().value();
        return codePersistenceRepository.findAll().stream()
                .filter(entity -> entity.getCodeHash().equals(fingerprint))
                .findFirst()
                .orElseThrow();
    }

    private BigDecimal committedArea(CooperativeLicense owner) {
        var id = UUID.fromString(owner.snapshot().id().cooperativeLicenseId());
        return licensePersistenceRepository.findById(id).orElseThrow().getCommittedAreaHa();
    }

    private List<SubscriptionPersistenceEntity> subscriptionsOf(String producerId) {
        return subscriptionPersistenceRepository.findAll().stream()
                .filter(subscription -> subscription.getProducerId().toString().equals(producerId))
                .toList();
    }

    private String signUp() {
        var email = "producer-" + UUID.randomUUID().toString().substring(0, 8) + "@viora.ag";
        var result = userAccountCommandService.handle(new RegisterUserAccountCommand(email, "Password123",
                "ROLE_PRODUCER", true, "Teodoro Mamani", "PE", "+51987654321"));
        assertThat(result).isInstanceOf(Result.Success.class);
        return ((Result.Success<String, ?>) result).value();
    }

    private static void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}

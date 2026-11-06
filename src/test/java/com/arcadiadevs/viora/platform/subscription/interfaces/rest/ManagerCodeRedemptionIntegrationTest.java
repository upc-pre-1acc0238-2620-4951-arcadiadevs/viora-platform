package com.arcadiadevs.viora.platform.subscription.interfaces.rest;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.ManagerCodeRedemptionCommandService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicenseSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemManagerCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.ManagerActivationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.ManagerActivationCodePersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.CooperativeLicensePersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.ManagerActivationCodePersistenceRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Manager code redemption and the manager's license against the whole application: real accounts, real codes, real
 * database.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:managercoderedemption;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Manager code redemption integration")
class ManagerCodeRedemptionIntegrationTest {

    private static final String REDEEM = "/api/v1/manager-code-redemptions";
    private static final String LICENSES = "/api/v1/cooperative-licenses";
    private static final String NOT_REDEEMABLE = "subscription.manager_code.not_redeemable";
    private static final String NOT_REDEEMABLE_EN = "Invalid or already used code. Ask the Viora team for a new one.";
    private static final String NOT_REDEEMABLE_ES = "Código no válido o ya usado. Pide uno nuevo al equipo de Viora.";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired WebApplicationContext context;
    @Autowired UserAccountCommandService userAccountCommandService;
    @Autowired ManagerCodeRedemptionCommandService redemptionCommandService;
    @Autowired CooperativeLicenseRepository licenseRepository;
    @Autowired ManagerActivationCodeRepository codeRepository;
    @Autowired CooperativeLicensePersistenceRepository licensePersistenceRepository;
    @Autowired ManagerActivationCodePersistenceRepository codePersistenceRepository;
    @Autowired PlatformTransactionManager transactionManager;

    private MockMvc mvc;
    private String manager;
    private CooperativeLicense license;
    private Instant licenseEnd;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        manager = signUp("ROLE_TECHNICAL_MANAGER");
        licenseEnd = Instant.now().plus(Duration.ofDays(400)).truncatedTo(ChronoUnit.SECONDS);
        license = newLicense(Instant.now().minus(Duration.ofDays(1)), licenseEnd);
        authenticateAs(manager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("A manager redeems a code: 201 with the license, which the manager then lists and reads by id")
    void redeemsACodeAndReadsTheLicense() throws Exception {
        var code = newCode(license);
        var id = license.snapshot().id().cooperativeLicenseId();
        mvc.perform(get(LICENSES)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        redeem(code)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, LICENSES + "/" + id))
                .andExpect(jsonPath("$.id").value(license.snapshot().id().cooperativeLicenseId()))
                .andExpect(jsonPath("$.organizationName").value("Cooperativa Olivar del Sur"))
                .andExpect(jsonPath("$.seats").value(60))
                .andExpect(jsonPath("$.contractedAreaHa").value(200.0))
                .andExpect(jsonPath("$.endsAt").value(licenseEnd.toString()))
                .andExpect(jsonPath("$.active").value(true));

        mvc.perform(get(LICENSES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].organizationName").value("Cooperativa Olivar del Sur"))
                .andExpect(jsonPath("$[0].active").value(true));
        mvc.perform(get(LICENSES + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats").value(60));
        var stored = storedCode(code);
        assertThat(stored.getStatus()).isEqualTo(ManagerCodeStatus.REDEEMED);
        assertThat(stored.getRedeemedBy()).hasToString(manager);
        assertThat(managerOf(license)).hasToString(manager);
    }

    @Test
    @DisplayName("The code is accepted in lowercase and with spaces around it")
    void codeIsNormalized() throws Exception {
        var code = newCode(license);

        redeem("  " + code.toLowerCase() + " ").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("An unknown, expired, revoked or used code, or one of an ended license, answers the same 400")
    void codesThatCannotBeRedeemed() throws Exception {
        var expired = newCode(license, Instant.now().minus(Duration.ofMinutes(1)));
        var revoked = newCode(license);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var code = codeRepository.findByFingerprintForRedemption(new ManagerCode(revoked).fingerprint())
                    .orElseThrow();
            code.revoke();
            codeRepository.save(code);
        });
        var used = newCode(license);
        authenticateAs(signUp("ROLE_TECHNICAL_MANAGER"));
        redeem(used).andExpect(status().isCreated());
        authenticateAs(manager);
        var ended = newLicense(Instant.now().minus(Duration.ofDays(400)), Instant.now().minus(Duration.ofDays(1)));
        var ofEndedLicense = newCode(ended, Instant.now().minus(Duration.ofDays(2)));

        for (var each : List.of("GEST-ZZZZ", expired, revoked, used, ofEndedLicense)) {
            redeem(each)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.reason").value(NOT_REDEEMABLE))
                    .andExpect(jsonPath("$.detail").value(NOT_REDEEMABLE_EN));
        }
        mvc.perform(get(LICENSES)).andExpect(jsonPath("$.length()").value(0));
        assertThat(storedCode(revoked).getStatus()).isEqualTo(ManagerCodeStatus.REVOKED);
    }

    @Test
    @DisplayName("The refusal is in Spanish for a Spanish speaking manager")
    void refusalIsLocalized() throws Exception {
        mvc.perform(post(REDEEM).header(HttpHeaders.ACCEPT_LANGUAGE, "es")
                        .contentType(MediaType.APPLICATION_JSON).content(body("GEST-ZZZZ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(NOT_REDEEMABLE_ES));
    }

    @Test
    @DisplayName("A manager in charge of a license in force gets 409 and the second code stays available")
    void managerAlreadyLicensed() throws Exception {
        redeem(newCode(license)).andExpect(status().isCreated());
        var other = newLicense(Instant.now().minus(Duration.ofDays(1)), licenseEnd);
        var second = newCode(other);

        redeem(second)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("subscription.manager.already_licensed"));

        assertThat(storedCode(second).getStatus()).isEqualTo(ManagerCodeStatus.AVAILABLE);
        assertThat(managerOf(other)).isNull();
    }

    @Test
    @DisplayName("A manager whose license ended reads it as inactive, and can take a new one, leaving the old one")
    void managerOfAnEndedLicense() throws Exception {
        var ended = licenseRepository.save(CooperativeLicense.reconstitute(new CooperativeLicenseSnapshot(
                new CooperativeLicenseId(), new CooperativeId(), "Coop. Anterior", 10,
                new HectaresQuota(new BigDecimal("50")), BigDecimal.ZERO,
                new SubscriptionPeriod(Instant.now().minus(Duration.ofDays(400)), Instant.now().minus(Duration.ofDays(1))),
                new ManagerId(manager), 0L)));
        mvc.perform(get(LICENSES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organizationName").value("Coop. Anterior"))
                .andExpect(jsonPath("$[0].active").value(false));

        redeem(newCode(license)).andExpect(status().isCreated());

        assertThat(managerOf(ended)).isNull();
        mvc.perform(get(LICENSES)).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].organizationName").value("Cooperativa Olivar del Sur"));
    }

    @Test
    @DisplayName("A new code of the same license replaces the manager: the previous one no longer has a license")
    void anotherManagerTakesOver() throws Exception {
        redeem(newCode(license)).andExpect(status().isCreated());
        var replacement = signUp("ROLE_TECHNICAL_MANAGER");
        authenticateAs(replacement);

        redeem(newCode(license)).andExpect(status().isCreated());

        assertThat(managerOf(license)).hasToString(replacement);
        authenticateAs(manager);
        mvc.perform(get(LICENSES)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(LICENSES + "/" + license.snapshot().id().cooperativeLicenseId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Each manager only reads their own license: another one, or one that does not exist, answers 404")
    void managersOnlyReadTheirOwnLicense() throws Exception {
        redeem(newCode(license)).andExpect(status().isCreated());
        var other = newLicense(Instant.now().minus(Duration.ofDays(1)), licenseEnd);
        authenticateAs(signUp("ROLE_TECHNICAL_MANAGER"));
        redeem(newCode(other)).andExpect(status().isCreated());

        mvc.perform(get(LICENSES + "/" + license.snapshot().id().cooperativeLicenseId()))
                .andExpect(status().isNotFound());
        mvc.perform(get(LICENSES + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get(LICENSES)).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(other.snapshot().id().cooperativeLicenseId()));
    }

    @Test
    @DisplayName("A producer cannot redeem a manager code, and the code stays available")
    void producersCannotRedeem() throws Exception {
        var code = newCode(license);
        authenticateAs(signUp("ROLE_PRODUCER"));

        redeem(code).andExpect(status().isForbidden());

        assertThat(storedCode(code).getStatus()).isEqualTo(ManagerCodeStatus.AVAILABLE);
    }

    @Test
    @DisplayName("A malformed code answers 400 with the format and does not use up attempts")
    void malformedCode() throws Exception {
        for (var attempt = 0; attempt < 6; attempt++) {
            redeem("COOP-7K2Q")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail")
                            .value("The code must be GEST- followed by four letters or digits, for example GEST-7K2Q"));
        }
        redeem(newCode(license)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("After five codes that cannot be redeemed the manager waits, even for a valid code")
    void tooManyFailedAttempts() throws Exception {
        for (var attempt = 0; attempt < 5; attempt++) {
            redeem(randomCode()).andExpect(status().isBadRequest());
        }
        var valid = newCode(license);

        redeem(valid)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.code").value("MANAGER_CODE_REDEMPTION_TOO_MANY_REQUESTS"));

        assertThat(storedCode(valid).getStatus()).isEqualTo(ManagerCodeStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Without a session nothing is redeemed nor read")
    void noSession() throws Exception {
        var code = newCode(license);
        SecurityContextHolder.clearContext();

        redeem(code).andExpect(status().isUnauthorized());
        mvc.perform(get(LICENSES)).andExpect(status().isUnauthorized());
        assertThat(storedCode(code).getStatus()).isEqualTo(ManagerCodeStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Two managers redeeming the same code at the same time: one gets the license, the other the 400")
    void concurrentRedemptionsOfTheSameCode() throws Exception {
        var code = newCode(license);
        var managers = List.of(signUp("ROLE_TECHNICAL_MANAGER"), signUp("ROLE_TECHNICAL_MANAGER"));
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var futures = new ArrayList<Future<Result<CooperativeLicense, ApplicationError>>>();
            for (var each : managers) {
                Callable<Result<CooperativeLicense, ApplicationError>> task = () -> {
                    start.await();
                    return redemptionCommandService.handle(new RedeemManagerCodeCommand(each, code));
                };
                futures.add(executor.submit(task));
            }
            start.countDown();
            var results = new ArrayList<Result<CooperativeLicense, ApplicationError>>();
            for (var future : futures) {
                results.add(future.get());
            }

            assertThat(results).filteredOn(Result::isSuccess).hasSize(1);
            assertThat(results).filteredOn(Result::isFailure).singleElement().satisfies(failure ->
                    assertThat(failure.failure().orElseThrow().details()).isEqualTo(NOT_REDEEMABLE));
        } finally {
            executor.shutdownNow();
        }
        assertThat(managers).contains(String.valueOf(managerOf(license)));
    }

    private ResultActions redeem(String code) throws Exception {
        return mvc.perform(post(REDEEM).contentType(MediaType.APPLICATION_JSON).content(body(code)));
    }

    private static String body(String code) {
        return "{\"code\": \"" + code + "\"}";
    }

    private CooperativeLicense newLicense(Instant start, Instant end) {
        return licenseRepository.save(CooperativeLicense.grant(new CooperativeId(), "Cooperativa Olivar del Sur", 60,
                new HectaresQuota(new BigDecimal("200")), new SubscriptionPeriod(start, end)));
    }

    private String newCode(CooperativeLicense owner) {
        return newCode(owner, owner.snapshot().period().endsAt());
    }

    private String newCode(CooperativeLicense owner, Instant expiresAt) {
        var code = randomCode();
        codeRepository.save(owner.issueManagerCode(new ManagerCode(code).fingerprint(), expiresAt));
        return code;
    }

    private static String randomCode() {
        var alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        var code = new StringBuilder("GEST-");
        for (var index = 0; index < 4; index++) {
            code.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return code.toString();
    }

    private ManagerActivationCodePersistenceEntity storedCode(String code) {
        var fingerprint = new ManagerCode(code).fingerprint().value();
        return codePersistenceRepository.findAll().stream()
                .filter(entity -> entity.getCodeHash().equals(fingerprint))
                .findFirst()
                .orElseThrow();
    }

    private UUID managerOf(CooperativeLicense owner) {
        var id = UUID.fromString(owner.snapshot().id().cooperativeLicenseId());
        return licensePersistenceRepository.findById(id).orElseThrow().getManagerUserId();
    }

    private String signUp(String role) {
        var email = "user-" + UUID.randomUUID().toString().substring(0, 8) + "@viora.ag";
        var result = userAccountCommandService.handle(new RegisterUserAccountCommand(email, "Password123",
                role, true, "Rubén Ticona", "PE", "+51951406118"));
        assertThat(result).isInstanceOf(Result.Success.class);
        return ((Result.Success<String, ?>) result).value();
    }

    private static void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}

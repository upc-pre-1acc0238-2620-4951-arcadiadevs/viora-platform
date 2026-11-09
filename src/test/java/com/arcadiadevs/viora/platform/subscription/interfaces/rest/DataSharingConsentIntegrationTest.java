package com.arcadiadevs.viora.platform.subscription.interfaces.rest;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicenseSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.DataSharingConsentPersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.purge.SubscriptionAccountDataPurger;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.DataSharingConsentPersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.InvitationCodePersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionPersistenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Preview of a cooperative code, redemption with and without the consent to share data, and stopping to share,
 * against the whole application.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:datasharingconsent;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Data sharing consent integration")
class DataSharingConsentIntegrationTest {

    private static final String PREVIEWS = "/api/v1/cooperative-code-previews";
    private static final String REDEMPTIONS = "/api/v1/cooperative-code-redemptions";
    private static final String CONSENTS = "/api/v1/data-sharing-consents";
    private static final String VERSION = "2026-11-v1";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired WebApplicationContext context;
    @Autowired UserAccountCommandService userAccountCommandService;
    @Autowired CooperativeLicenseRepository licenseRepository;
    @Autowired InvitationCodeRepository invitationCodeRepository;
    @Autowired InvitationCodePersistenceRepository codeRows;
    @Autowired SubscriptionPersistenceRepository subscriptionRows;
    @Autowired DataSharingConsentPersistenceRepository consentRows;
    @Autowired SubscriptionAccountDataPurger purger;

    private MockMvc mvc;
    private String producer;
    private CooperativeLicense license;
    private Instant licenseEnd;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        var manager = signUp("ROLE_TECHNICAL_MANAGER", "Rubén Ticona");
        producer = signUp("ROLE_PRODUCER", "Teodoro Mamani");
        licenseEnd = Instant.now().plus(Duration.ofDays(120)).truncatedTo(ChronoUnit.SECONDS);
        license = licenseRepository.save(CooperativeLicense.reconstitute(new CooperativeLicenseSnapshot(
                new CooperativeLicenseId(), new CooperativeId(), "Cooperativa Olivar del Sur", 60,
                new HectaresQuota(new BigDecimal("200")), BigDecimal.ZERO,
                new SubscriptionPeriod(Instant.now().minus(Duration.ofDays(1)), licenseEnd),
                new ManagerId(manager), 0L)));
        authenticateAs(producer);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Previewing shows what the code offers and leaves it available")
    void previewDoesNotConsumeTheCode() throws Exception {
        var code = newCode("12");

        preview(code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("Cooperativa Olivar del Sur"))
                .andExpect(jsonPath("$.managerName").value("Rubén Ticona"))
                .andExpect(jsonPath("$.quotaHa").value(12.0))
                .andExpect(jsonPath("$.planEndsAt").value(licenseEnd.toString()))
                .andExpect(jsonPath("$.registeredAreaHa").value(0.0))
                .andExpect(jsonPath("$.exceedsQuota").value(false))
                .andExpect(jsonPath("$.consentTextVersion").value(VERSION));
        preview(code).andExpect(status().isOk());

        assertThat(storedStatus(code)).isEqualTo(InvitationCodeStatus.AVAILABLE);
        assertThat(subscriptionRows.findAll())
                .noneMatch(row -> row.getProducerId().toString().equals(producer));
    }

    @Test
    @DisplayName("Previewing a code that cannot be redeemed, or with a plan already active, answers with its reason")
    void previewRefusalsCarryTheirReason() throws Exception {
        preview(randomCode())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("subscription.invitation_code.not_redeemable"));

        redeem(newCode("5"), VERSION).andExpect(status().isCreated());

        preview(newCode("5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("subscription.producer.already_active"));
    }

    @Test
    @DisplayName("Redeeming with the text version records the consent with the plan; listing shows it active")
    void redeemingWithConsentRecordsIt() throws Exception {
        redeem(newCode("12"), VERSION).andExpect(status().isCreated());

        var consents = consentsOf(producer);
        assertThat(consents).singleElement().satisfies(row -> {
            assertThat(row.getTextVersion()).isEqualTo(VERSION);
            assertThat(row.getEndsAt()).isEqualTo(licenseEnd);
            assertThat(row.getWithdrawnAt()).isNull();
        });
        mvc.perform(get(CONSENTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(consents.getFirst().getId().toString()))
                .andExpect(jsonPath("$[0].organizationName").value("Cooperativa Olivar del Sur"))
                .andExpect(jsonPath("$[0].managerName").value("Rubén Ticona"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].planEndsAt").value(licenseEnd.toString()))
                .andExpect(jsonPath("$[0].consentTextVersion").value(VERSION));
    }

    @Test
    @DisplayName("Redeeming without the text version is refused and leaves the code ready to be redeemed")
    void redeemingWithoutConsent() throws Exception {
        var code = newCode("12");

        redeem(code, null).andExpect(status().isBadRequest());
        redeem(code, " ").andExpect(status().isBadRequest());

        assertThat(consentsOf(producer)).isEmpty();
        redeem(code, VERSION).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("An outdated text version answers 409 with the version in force and redeems nothing")
    void outdatedTextRedeemsNothing() throws Exception {
        var code = newCode("12");

        redeem(code, "2025-01-v1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("subscription.consent.text_outdated"))
                .andExpect(jsonPath("$.consentTextVersion").value(VERSION));

        assertThat(storedStatus(code)).isEqualTo(InvitationCodeStatus.AVAILABLE);
        assertThat(consentsOf(producer)).isEmpty();
    }

    @Test
    @DisplayName("Stopping to share is for good: the second time answers 409 and the plan stays active")
    void withdrawIsForGood() throws Exception {
        redeem(newCode("12"), VERSION).andExpect(status().isCreated());
        var id = consentsOf(producer).getFirst().getId();

        mvc.perform(post(CONSENTS + "/" + id + "/withdrawals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.withdrawnAt").isNotEmpty());
        mvc.perform(post(CONSENTS + "/" + id + "/withdrawals"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("subscription.consent.not_active"));

        mvc.perform(get(CONSENTS + "/" + id)).andExpect(jsonPath("$.status").value("WITHDRAWN"));
        assertThat(subscriptionRows.findAll())
                .filteredOn(row -> row.getProducerId().toString().equals(producer))
                .singleElement()
                .satisfies(row -> assertThat(row.getStatus().name()).isEqualTo("ACTIVE"));
    }

    @Test
    @DisplayName("A consent of another producer answers 404, as one that does not exist, and is not touched")
    void consentOfAnotherProducer() throws Exception {
        redeem(newCode("12"), VERSION).andExpect(status().isCreated());
        var id = consentsOf(producer).getFirst().getId();
        authenticateAs(signUp("ROLE_PRODUCER", "Hilda Poma"));

        mvc.perform(get(CONSENTS + "/" + id)).andExpect(status().isNotFound());
        mvc.perform(post(CONSENTS + "/" + id + "/withdrawals")).andExpect(status().isNotFound());
        mvc.perform(post(CONSENTS + "/" + UUID.randomUUID() + "/withdrawals")).andExpect(status().isNotFound());
        mvc.perform(get(CONSENTS)).andExpect(jsonPath("$.length()").value(0));

        assertThat(consentRows.findById(id).orElseThrow().getWithdrawnAt()).isNull();
    }

    @Test
    @DisplayName("Deleting the producer's account removes their consents")
    void purgeRemovesConsents() throws Exception {
        redeem(newCode("12"), VERSION).andExpect(status().isCreated());
        assertThat(consentsOf(producer)).hasSize(1);

        purger.on(new UserAccountDeletedEvent(producer, "deleted@viora.ag", Instant.now()));

        assertThat(consentsOf(producer)).isEmpty();
    }

    private ResultActions preview(String code) throws Exception {
        return mvc.perform(post(PREVIEWS).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\": \"" + code + "\"}"));
    }

    private ResultActions redeem(String code, String version) throws Exception {
        var body = version == null
                ? "{\"code\": \"" + code + "\"}"
                : "{\"code\": \"" + code + "\", \"consentTextVersion\": \"" + version + "\"}";
        return mvc.perform(post(REDEMPTIONS).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String newCode(String hectares) {
        var code = randomCode();
        var stored = licenseRepository.findById(license.snapshot().id()).orElseThrow();
        var issued = stored.issueCode(new CooperativeCode(code).fingerprint(),
                new HectaresQuota(new BigDecimal(hectares)), Instant.now().plus(Duration.ofDays(7)));
        licenseRepository.save(stored);
        invitationCodeRepository.save(issued);
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

    private InvitationCodeStatus storedStatus(String code) {
        var fingerprint = new CooperativeCode(code).fingerprint().value();
        return codeRows.findAll().stream()
                .filter(row -> row.getCodeHash().equals(fingerprint))
                .findFirst().orElseThrow().getStatus();
    }

    private List<DataSharingConsentPersistenceEntity> consentsOf(String producerId) {
        return consentRows.findAllByProducerIdOrderByAcceptedAtDesc(UUID.fromString(producerId));
    }

    private String signUp(String role, String fullName) {
        var email = "user-" + UUID.randomUUID().toString().substring(0, 8) + "@viora.ag";
        var result = userAccountCommandService.handle(new RegisterUserAccountCommand(email, "Password123",
                role, true, fullName, "PE", "+51951406118"));
        assertThat(result).isInstanceOf(Result.Success.class);
        return ((Result.Success<String, ?>) result).value();
    }

    private static void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}

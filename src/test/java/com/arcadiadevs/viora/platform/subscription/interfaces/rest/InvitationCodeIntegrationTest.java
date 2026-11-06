package com.arcadiadevs.viora.platform.subscription.interfaces.rest;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
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
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.InvitationCodePersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.CooperativeLicensePersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.InvitationCodePersistenceRepository;
import com.jayway.jsonpath.JsonPath;
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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Invitation codes a technical manager issues, lists and revokes, against the whole application: real accounts, real
 * licenses, real database.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:invitationcodes;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Invitation codes integration")
class InvitationCodeIntegrationTest {

    private static final String CODES = "/api/v1/invitation-codes";
    private static final String LICENSES = "/api/v1/cooperative-licenses";

    @Autowired WebApplicationContext context;
    @Autowired UserAccountCommandService userAccountCommandService;
    @Autowired CooperativeLicenseRepository licenseRepository;
    @Autowired InvitationCodeRepository codeRepository;
    @Autowired CooperativeLicensePersistenceRepository licenseRows;
    @Autowired InvitationCodePersistenceRepository codeRows;

    private MockMvc mvc;
    private String manager;
    private CooperativeLicense license;
    private Instant licenseEnd;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        manager = signUp("ROLE_TECHNICAL_MANAGER", "Rubén Ticona");
        licenseEnd = Instant.now().plus(Duration.ofDays(400)).truncatedTo(ChronoUnit.SECONDS);
        license = licenseOf(manager, 60, "200", Instant.now().minus(Duration.ofDays(1)), licenseEnd);
        authenticateAs(manager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("A batch comes back in full, holds its seats and hectares, and lists as available")
    void issuesABatch() throws Exception {
        var body = issue(5, "5", 30)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, CODES + "?status=available"))
                .andExpect(jsonPath("$.codes.length()").value(5))
                .andExpect(jsonPath("$.hectaresPerCode").value(5.0))
                .andReturn().getResponse().getContentAsString();
        List<String> issued = JsonPath.read(body, "$.codes[*].code");
        Instant expiresAt = Instant.parse(JsonPath.read(body, "$.expiresAt"));

        assertThat(issued).doesNotHaveDuplicates().allMatch(code -> code.matches("COOP-[A-Z2-9]{4}"));
        assertThat(Duration.between(Instant.now(), expiresAt).toDays()).isBetween(29L, 30L);
        mvc.perform(get(CODES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].hectares").value(5.0));
        List<String> listed = JsonPath.read(mvc.perform(get(CODES)).andReturn().getResponse().getContentAsString(),
                "$[*].code");
        assertThat(listed).containsExactlyInAnyOrderElementsOf(issued);
        mvc.perform(get(LICENSES))
                .andExpect(jsonPath("$[0].seatsUsed").value(5))
                .andExpect(jsonPath("$[0].seatsFree").value(55))
                .andExpect(jsonPath("$[0].reservedAreaHa").value(25.0))
                .andExpect(jsonPath("$[0].freeAreaHa").value(175.0))
                .andExpect(jsonPath("$[0].suggestedHectaresPerCode").value(3.0));
        assertThat(rowsOf(license)).hasSize(5).allSatisfy(row -> {
            assertThat(row.getSealedCode()).isNotBlank();
            assertThat(issued).noneMatch(code -> row.getSealedCode().contains(code.substring(5)));
        });
    }

    @Test
    @DisplayName("Codes never outlive the license, whatever validity is chosen")
    void codesExpireWithTheLicense() throws Exception {
        var shortLicense = signUp("ROLE_TECHNICAL_MANAGER", "Rosa Condori");
        var ends = Instant.now().plus(Duration.ofDays(10)).truncatedTo(ChronoUnit.SECONDS);
        licenseOf(shortLicense, 10, "50", Instant.now().minus(Duration.ofDays(1)), ends);
        authenticateAs(shortLicense);

        issue(1, "5", 30).andExpect(status().isCreated()).andExpect(jsonPath("$.expiresAt").value(ends.toString()));
    }

    @Test
    @DisplayName("A batch beyond the free seats or hectares is refused with what is left, and changes nothing")
    void notEnoughSeatsOrHectares() throws Exception {
        var small = signUp("ROLE_TECHNICAL_MANAGER", "Julio Quispe");
        licenseOf(small, 3, "10", Instant.now().minus(Duration.ofDays(1)), licenseEnd);
        authenticateAs(small);

        issue(4, "1", 7)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("subscription.license.insufficient_seats"))
                .andExpect(jsonPath("$.seatsFree").value(3));
        issue(3, "4", 7)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("subscription.license.insufficient_area"))
                .andExpect(jsonPath("$.freeAreaHa").value(10.0));
        mvc.perform(get(CODES)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("A quantity, validity or hectares out of range answers 400")
    void invalidBatches() throws Exception {
        issue(0, "5", 30).andExpect(status().isBadRequest());
        issue(51, "5", 30).andExpect(status().isBadRequest());
        issue(1, "5", 10).andExpect(status().isBadRequest());
        issue(1, "0", 30).andExpect(status().isBadRequest());
        mvc.perform(get(CODES).param("status", "pending")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Revoking a code gives its seat and hectares back and closes it, masked")
    void revoking() throws Exception {
        var id = issuedIds(issue(2, "5", 15)).getFirst();

        mvc.perform(post(CODES + "/" + id + "/revocations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.maskedCode").value(startsWith("COOP-••")));

        mvc.perform(get(CODES)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(CODES).param("status", "closed"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("REVOKED"))
                .andExpect(jsonPath("$[0].code").doesNotExist());
        mvc.perform(get(LICENSES))
                .andExpect(jsonPath("$[0].seatsUsed").value(1))
                .andExpect(jsonPath("$[0].reservedAreaHa").value(5.0));
        mvc.perform(post(CODES + "/" + id + "/revocations"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("subscription.invitation_code.not_available"));
        assertThat(row(id).getSealedCode()).isNull();
    }

    @Test
    @DisplayName("A redeemed code cannot be revoked, and lists masked with the producer who used it")
    void aRedeemedCode() throws Exception {
        var body = issue(1, "5", 15).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.codes[0].id");
        String code = JsonPath.read(body, "$.codes[0].code");
        authenticateAs(signUp("ROLE_PRODUCER", "Teodoro Mamani"));
        mvc.perform(post("/api/v1/cooperative-code-redemptions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"" + code + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cooperativeName").value("Cooperativa Olivar del Sur"));
        authenticateAs(manager);

        mvc.perform(post(CODES + "/" + id + "/revocations"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("subscription.invitation_code.already_redeemed"))
                .andExpect(jsonPath("$.redeemedAt").exists());
        mvc.perform(get(CODES).param("status", "redeemed"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").doesNotExist())
                .andExpect(jsonPath("$[0].maskedCode").value("COOP-••" + code.substring(code.length() - 2)))
                .andExpect(jsonPath("$[0].redeemedBy").value("Teodoro Mamani"))
                .andExpect(jsonPath("$[0].redeemedAt").exists());
        mvc.perform(get(LICENSES)).andExpect(jsonPath("$[0].seatsUsed").value(1));
        assertThat(row(id).getSealedCode()).isNull();
    }

    @Test
    @DisplayName("A code whose time is over shows as closed and frees its seat, and the next batch marks it expired")
    void expiredCodes() throws Exception {
        var stored = licenseRepository.findById(license.snapshot().id()).orElseThrow();
        var overdue = stored.issueCode(new CooperativeCode("COOP-ZZ22").fingerprint(),
                new HectaresQuota(new BigDecimal("6")), Instant.now().minusSeconds(60));
        licenseRepository.save(stored);
        codeRepository.save(overdue);

        mvc.perform(get(CODES)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(CODES).param("status", "closed"))
                .andExpect(jsonPath("$[0].status").value("EXPIRED"))
                .andExpect(jsonPath("$[0].maskedCode").value("COOP-••••"));
        mvc.perform(get(LICENSES))
                .andExpect(jsonPath("$[0].seatsUsed").value(0))
                .andExpect(jsonPath("$[0].freeAreaHa").value(200.0));

        issue(1, "5", 7).andExpect(status().isCreated());

        assertThat(row(overdue.snapshot().id().invitationCodeId()).getStatus())
                .isEqualTo(InvitationCodeStatus.EXPIRED);
        assertThat(committedArea()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("Codes are unique among every license")
    void codesAreUniqueAmongLicenses() throws Exception {
        issue(30, "1", 30).andExpect(status().isCreated());
        var other = signUp("ROLE_TECHNICAL_MANAGER", "Hilda Poma");
        var otherLicense = licenseOf(other, 60, "200", Instant.now().minus(Duration.ofDays(1)), licenseEnd);
        authenticateAs(other);
        issue(30, "1", 30).andExpect(status().isCreated());

        assertThat(rowsOf(license)).hasSize(30);
        assertThat(rowsOf(otherLicense)).hasSize(30);
        assertThat(codeRows.findAll()).extracting(InvitationCodePersistenceEntity::getCodeHash)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("A manager only reaches the codes of their own license")
    void managersOnlyReachTheirCodes() throws Exception {
        var id = issuedIds(issue(1, "5", 7)).getFirst();
        var other = signUp("ROLE_TECHNICAL_MANAGER", "Hilda Poma");
        licenseOf(other, 10, "50", Instant.now().minus(Duration.ofDays(1)), licenseEnd);
        authenticateAs(other);

        mvc.perform(get(CODES)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post(CODES + "/" + id + "/revocations")).andExpect(status().isNotFound());
        mvc.perform(post(CODES + "/" + UUID.randomUUID() + "/revocations")).andExpect(status().isNotFound());
        assertThat(row(id).getStatus()).isEqualTo(InvitationCodeStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Without a license in force, or as a producer, no code is issued")
    void needsALicenseInForce() throws Exception {
        var before = codeRows.count();
        authenticateAs(signUp("ROLE_TECHNICAL_MANAGER", "Sin Licencia"));
        issue(1, "5", 7).andExpect(status().isForbidden());

        authenticateAs(signUp("ROLE_PRODUCER", "Teodoro Mamani"));
        issue(1, "5", 7).andExpect(status().isForbidden());

        SecurityContextHolder.clearContext();
        issue(1, "5", 7).andExpect(status().isUnauthorized());
        assertThat(codeRows.count()).isEqualTo(before);
    }

    private ResultActions issue(int quantity, String hectares, int validityDays) throws Exception {
        return mvc.perform(post(CODES).contentType(MediaType.APPLICATION_JSON).content(
                "{\"quantity\": %d, \"hectaresPerCode\": %s, \"validityDays\": %d}"
                        .formatted(quantity, hectares, validityDays)));
    }

    private static List<String> issuedIds(ResultActions issued) throws Exception {
        return JsonPath.read(issued.andReturn().getResponse().getContentAsString(), "$.codes[*].id");
    }

    private List<InvitationCodePersistenceEntity> rowsOf(CooperativeLicense owner) {
        var id = UUID.fromString(owner.snapshot().id().cooperativeLicenseId());
        return codeRows.findAll().stream().filter(row -> row.getLicenseId().equals(id)).toList();
    }

    private InvitationCodePersistenceEntity row(String id) {
        return codeRows.findById(UUID.fromString(id)).orElseThrow();
    }

    private BigDecimal committedArea() {
        return licenseRows.findById(UUID.fromString(license.snapshot().id().cooperativeLicenseId()))
                .orElseThrow().getCommittedAreaHa();
    }

    private CooperativeLicense licenseOf(String managerId, int seats, String hectares, Instant start, Instant end) {
        return licenseRepository.save(CooperativeLicense.reconstitute(new CooperativeLicenseSnapshot(
                new CooperativeLicenseId(), new CooperativeId(), "Cooperativa Olivar del Sur", seats,
                new HectaresQuota(new BigDecimal(hectares)), BigDecimal.ZERO, new SubscriptionPeriod(start, end),
                new ManagerId(managerId), 0L)));
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

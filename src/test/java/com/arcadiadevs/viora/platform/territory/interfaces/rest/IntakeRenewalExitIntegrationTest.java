package com.arcadiadevs.viora.platform.territory.interfaces.rest;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.orchard.application.commandservices.PlotCommandService;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CooperativeCodeRedemptionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CooperativeLicenseCommandService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicenseSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemCooperativeCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RenewCooperativeLicenseCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.InvitationCodePersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionPersistenceRepository;
import com.arcadiadevs.viora.platform.territory.application.commandservices.PlotSectorAssignmentCommandService;
import com.arcadiadevs.viora.platform.territory.application.commandservices.SectorZoneCommandService;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.AssignPlotSectorCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.CreateSectorZoneCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Intake projection, member release, account deletion and license renewal against the whole application. The grace
 * of a released member is zero here, so their seat goes back at once.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:territoryintakerenewalexit;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "viora.subscription.member-release.grace-days=0"
})
@DisplayName("Intake, member release, account deletion and renewal integration")
class IntakeRenewalExitIntegrationTest {

    private static final String LICENSES = "/api/v1/cooperative-licenses";
    private static final String INTAKES = "/api/v1/intake-forecasts";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired WebApplicationContext context;
    @Autowired UserAccountCommandService userAccountCommandService;
    @Autowired CooperativeLicenseRepository licenseRepository;
    @Autowired InvitationCodeRepository invitationCodeRepository;
    @Autowired CooperativeCodeRedemptionCommandService redemptionCommandService;
    @Autowired PlotCommandService plotCommandService;
    @Autowired SectorZoneCommandService sectorZoneCommandService;
    @Autowired PlotSectorAssignmentCommandService plotSectorAssignmentCommandService;
    @Autowired CooperativeLicenseCommandService cooperativeLicenseCommandService;
    @Autowired InvitationCodePersistenceRepository codeRows;
    @Autowired SubscriptionPersistenceRepository subscriptionRows;
    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired TransactionTemplate transactionTemplate;

    private MockMvc mvc;
    private String manager;
    private CooperativeLicense license;
    private String licenseId;
    private String member;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        manager = signUp("ROLE_TECHNICAL_MANAGER", "Rubén Ticona");
        license = licenseOf(manager);
        licenseId = license.snapshot().id().cooperativeLicenseId();
        member = member(license, "Teodoro Mamani");
        var plot = newPlot(member, "La Yarada 02", -70.25);
        newPlot(member, "Lote Norte", -70.30);
        var zone = sectorZoneCommandService.handle(new CreateSectorZoneCommand(manager, "La Yarada Baja"))
                .success().orElseThrow().snapshot().id().sectorZoneId();
        assertThat(plotSectorAssignmentCommandService.handle(
                new AssignPlotSectorCommand(member, plot, zone, null)).isSuccess()).isTrue();
        authenticateAs(manager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("The intake of the campaign in progress counts the members' plots, by sector, without a figure yet")
    void intakeWithoutSampling() throws Exception {
        var current = LocalDate.now(ProducerTimeZone.ZONE).getYear();

        mvc.perform(get(INTAKES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].campaignYear").value(current))
                .andExpect(jsonPath("$[0].kind").value("PROJECTION"))
                .andExpect(jsonPath("$[0].status").value("NO_DATA"))
                .andExpect(jsonPath("$[0].plots").value(2))
                .andExpect(jsonPath("$[0].totalTons").doesNotExist());
        mvc.perform(get(INTAKES + "/" + current))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preliminaryThresholdPercent").value(50.0))
                .andExpect(jsonPath("$.coveragePercent").value(0.0))
                .andExpect(jsonPath("$.sectors.length()").value(2))
                .andExpect(jsonPath("$.sectors[0].sectorZoneName").value("La Yarada Baja"))
                .andExpect(jsonPath("$.sectors[0].plots").value(1))
                .andExpect(jsonPath("$.sectors[0].greenTons").doesNotExist())
                .andExpect(jsonPath("$.sectors[0].blackTons").doesNotExist())
                .andExpect(jsonPath("$.sectors[1].sectorStatus").value("NONE"));
        mvc.perform(get(INTAKES + "/" + (current + 1))).andExpect(status().isNotFound());
        mvc.perform(get(INTAKES + "/1999")).andExpect(status().isBadRequest());

        authenticateAs(member);
        mvc.perform(get(INTAKES)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Releasing a member ends the reading at once; past the grace the seat and hectares go back")
    void releaseMember() throws Exception {
        mvc.perform(get(LICENSES + "/" + licenseId))
                .andExpect(jsonPath("$.seatsUsed").value(1))
                .andExpect(jsonPath("$.reservedAreaHa").value(12.0));

        mvc.perform(post(LICENSES + "/" + licenseId + "/member-releases").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"" + member + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(member))
                .andExpect(jsonPath("$.readingEndedAt").isNotEmpty())
                .andExpect(jsonPath("$.seatFreesAt").isNotEmpty());

        mvc.perform(get("/api/v1/member-plots")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(LICENSES + "/" + licenseId))
                .andExpect(jsonPath("$.seatsUsed").value(0))
                .andExpect(jsonPath("$.reservedAreaHa").value(0.0));
        mvc.perform(post(LICENSES + "/" + licenseId + "/member-releases").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"" + member + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Another manager, an unknown member or a blank one cannot be released")
    void releaseRefusals() throws Exception {
        var otherManager = signUp("ROLE_TECHNICAL_MANAGER", "Rosa Condori");
        licenseOf(otherManager);

        mvc.perform(post(LICENSES + "/" + licenseId + "/member-releases").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(LICENSES + "/" + licenseId + "/member-releases").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \" \"}"))
                .andExpect(status().isBadRequest());
        authenticateAs(otherManager);
        mvc.perform(post(LICENSES + "/" + licenseId + "/member-releases").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"" + member + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A member who deletes their account gives their seat and hectares back at once")
    void accountDeletionReleasesTheSeat() throws Exception {
        // IAM publishes the deletion inside its own transaction.
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(
                new UserAccountDeletedEvent(member, "deleted@viora.ag", Instant.now())));

        assertThat(codeRows.findAll()).filteredOn(row -> member.equals(String.valueOf(row.getRedeemedBy())))
                .singleElement()
                .satisfies(row -> assertThat(row.getStatus()).isEqualTo(InvitationCodeStatus.RELEASED));
        mvc.perform(get(LICENSES + "/" + licenseId))
                .andExpect(jsonPath("$.seatsUsed").value(0))
                .andExpect(jsonPath("$.reservedAreaHa").value(0.0));
    }

    @Test
    @DisplayName("Renewing the license extends its members' plans without new codes")
    void renewalExtendsPlans() {
        var previousEnd = license.snapshot().period().endsAt();
        var newEnd = previousEnd.plus(Duration.ofDays(365)).truncatedTo(ChronoUnit.SECONDS);

        assertThat(cooperativeLicenseCommandService.handle(new RenewCooperativeLicenseCommand(licenseId, newEnd))
                .isSuccess()).isTrue();

        assertThat(subscriptionRows.findAll())
                .filteredOn(row -> row.getProducerId().toString().equals(member))
                .singleElement()
                .satisfies(row -> assertThat(row.getEndsAt()).isEqualTo(newEnd));
        assertThat(cooperativeLicenseCommandService.handle(new RenewCooperativeLicenseCommand(licenseId, previousEnd))
                .isFailure()).isTrue();
    }

    private CooperativeLicense licenseOf(String managerId) {
        return licenseRepository.save(CooperativeLicense.reconstitute(new CooperativeLicenseSnapshot(
                new CooperativeLicenseId(), new CooperativeId(), "Cooperativa Olivar del Sur", 60,
                new HectaresQuota(new BigDecimal("200")), BigDecimal.ZERO,
                new SubscriptionPeriod(Instant.now().minus(Duration.ofDays(1)),
                        Instant.now().plus(Duration.ofDays(120))),
                new ManagerId(managerId), 0L)));
    }

    private String member(CooperativeLicense owner, String fullName) {
        var producer = signUp("ROLE_PRODUCER", fullName);
        var code = randomCode();
        var stored = licenseRepository.findById(owner.snapshot().id()).orElseThrow();
        var issued = stored.issueCode(new CooperativeCode(code).fingerprint(), new HectaresQuota(new BigDecimal("12")),
                Instant.now().plus(Duration.ofDays(7)));
        licenseRepository.save(stored);
        invitationCodeRepository.save(issued);
        var command = new RedeemCooperativeCodeCommand(producer, code, "2026-11-v1");
        assertThat(redemptionCommandService.handle(command).isSuccess()).isTrue();
        return producer;
    }

    private String newPlot(String producerId, String name, double lng) {
        var geometry = "{\"type\":\"Polygon\",\"coordinates\":[[[" + lng + ",-18.05],[" + (lng + 0.001) + ",-18.05],["
                + (lng + 0.001) + ",-18.051],[" + lng + ",-18.051],[" + lng + ",-18.05]]]}";
        var result = plotCommandService.handle(new DelimitPlotCommand(producerId, name, "SEVILLANA", geometry, 7.0,
                7.0));
        assertThat(result.isSuccess()).as(String.valueOf(result.failure())).isTrue();
        return result.success().orElseThrow();
    }

    private static String randomCode() {
        var alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        var code = new StringBuilder("COOP-");
        for (var index = 0; index < 4; index++) {
            code.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return code.toString();
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

package com.arcadiadevs.viora.platform.territory.interfaces.rest;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.orchard.application.commandservices.PlotCommandService;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CooperativeCodeRedemptionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.DataSharingConsentCommandService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicenseSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemCooperativeCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.WithdrawDataSharingConsentCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.DataSharingConsentPersistenceRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.ThresholdBreachInfo;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import com.arcadiadevs.viora.platform.territory.application.commandservices.PlotSectorAssignmentCommandService;
import com.arcadiadevs.viora.platform.territory.application.commandservices.SectorAlertCommandService;
import com.arcadiadevs.viora.platform.territory.application.commandservices.SectorZoneCommandService;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.AssignPlotSectorCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.CreateSectorZoneCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.EvaluateSectorAlertsCommand;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The manager's provisional dossier and alert center against the whole application: members who share and who
 * stopped, real plots and sectors, and real agroclimatic incidents.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:territorydossieralerts;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Manager provisional dossier and sector alerts integration")
class DossierAndAlertsIntegrationTest {

    private static final String PLOTS = "/api/v1/member-plots";
    private static final String ALERTS = "/api/v1/sector-alerts";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired WebApplicationContext context;
    @Autowired UserAccountCommandService userAccountCommandService;
    @Autowired CooperativeLicenseRepository licenseRepository;
    @Autowired InvitationCodeRepository invitationCodeRepository;
    @Autowired CooperativeCodeRedemptionCommandService redemptionCommandService;
    @Autowired DataSharingConsentCommandService consentCommandService;
    @Autowired DataSharingConsentPersistenceRepository consentRows;
    @Autowired PlotCommandService plotCommandService;
    @Autowired SectorZoneCommandService sectorZoneCommandService;
    @Autowired PlotSectorAssignmentCommandService plotSectorAssignmentCommandService;
    @Autowired AgroclimaticIncidentRepository incidentRepository;
    @Autowired SectorAlertCommandService sectorAlertCommandService;

    private MockMvc mvc;
    private String manager;
    private CooperativeLicense license;
    private String sharing;
    private String hotPlot;
    private String coldPlot;
    private String withdrawn;
    private String withdrawnPlot;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        manager = signUp("ROLE_TECHNICAL_MANAGER", "Rubén Ticona");
        license = licenseOf(manager);
        sharing = member(license, "Teodoro Mamani");
        hotPlot = newPlot(sharing, "La Yarada 02", -70.25);
        coldPlot = newPlot(sharing, "Lote Norte", -70.30);
        withdrawn = member(license, "Hilda Poma");
        withdrawnPlot = newPlot(withdrawn, "Lote Sur", -70.27);
        withdraw(withdrawn);
        var zone = sectorZoneCommandService.handle(new CreateSectorZoneCommand(manager, "La Yarada Baja"))
                .success().orElseThrow().snapshot().id().sectorZoneId();
        for (var plot : List.of(hotPlot, coldPlot)) {
            assertThat(plotSectorAssignmentCommandService.handle(
                    new AssignPlotSectorCommand(sharing, plot, zone, null)).isSuccess()).isTrue();
        }
        authenticateAs(manager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("The provisional dossier reads as data or downloads as a PDF, only for a plot of a member who shares")
    void provisionalDossier() throws Exception {
        mvc.perform(get(PLOTS + "/" + hotPlot + "/provisional-dossier").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("PROVISIONAL"))
                .andExpect(jsonPath("$.plotName").value("La Yarada 02"))
                .andExpect(jsonPath("$.producerName").value("Teodoro Mamani"))
                .andExpect(jsonPath("$.organizationName").value("Cooperativa Olivar del Sur"))
                .andExpect(jsonPath("$.campaignYear").isNumber())
                .andExpect(jsonPath("$.cutOffAt").isNotEmpty())
                .andExpect(jsonPath("$.sampledTrees").value(0))
                .andExpect(jsonPath("$.thinnedOn").doesNotExist());

        var pdf = mvc.perform(get(PLOTS + "/" + hotPlot + "/provisional-dossier")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment; filename=\"expediente-provisional-")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");

        mvc.perform(get(PLOTS + "/" + withdrawnPlot + "/provisional-dossier").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        mvc.perform(get(PLOTS + "/" + withdrawnPlot + "/provisional-dossier").accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isNotFound());
        authenticateAs(sharing);
        mvc.perform(get(PLOTS + "/" + hotPlot + "/provisional-dossier").accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A critical incident opens a critical sector alert and frost an «Atención» one, without duplicates")
    void alertsOpenOnceAndNormalize() throws Exception {
        var heat = raise(hotPlot, IncidentType.HYDRIC_STRESS, IncidentSeverity.CRITICAL);
        raise(coldPlot, IncidentType.FROST_WARNING, IncidentSeverity.WARNING);

        assertThat(evaluate()).isEqualTo(2);
        assertThat(evaluate()).isZero();
        mvc.perform(get(ALERTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("SECTOR_CRITICAL"))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].sectorZoneName").value("La Yarada Baja"))
                .andExpect(jsonPath("$[0].affectedPlots").value(1))
                .andExpect(jsonPath("$[0].sectorPlots").value(2))
                .andExpect(jsonPath("$[0].leadPlotName").value("La Yarada 02"))
                .andExpect(jsonPath("$[0].leadProducerName").value("Teodoro Mamani"))
                .andExpect(jsonPath("$[0].leadReason").value("CRITICAL_INCIDENT"))
                .andExpect(jsonPath("$[1].type").value("FROST"))
                .andExpect(jsonPath("$[1].severity").value("ATTENTION"))
                .andExpect(jsonPath("$[1].leadPlotName").value("Lote Norte"));

        heat.normalize(Instant.now());
        incidentRepository.save(heat);
        assertThat(evaluate()).isEqualTo(1);

        mvc.perform(get(ALERTS))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("FROST"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].type").value("SECTOR_CRITICAL"))
                .andExpect(jsonPath("$[1].status").value("NORMALIZED"))
                .andExpect(jsonPath("$[1].normalizedAt").isNotEmpty())
                .andExpect(jsonPath("$[1].sectorLevelAfter").value("MODERATE"));
    }

    @Test
    @DisplayName("When the member stops sharing, their alerts normalize and no longer name them")
    void stoppingToShareNormalizesAndHides() throws Exception {
        raise(hotPlot, IncidentType.HYDRIC_STRESS, IncidentSeverity.CRITICAL);
        evaluate();

        withdraw(sharing);
        assertThat(evaluate()).isEqualTo(1);

        mvc.perform(get(ALERTS))
                .andExpect(jsonPath("$[0].status").value("NORMALIZED"))
                .andExpect(jsonPath("$[0].sectorLevelAfter").value("NO_DATA"))
                .andExpect(jsonPath("$[0].leadPlotId").doesNotExist())
                .andExpect(jsonPath("$[0].leadProducerName").doesNotExist());
    }

    @Test
    @DisplayName("Another manager sees no alert of this license, and a producer reaches none")
    void otherUsersReachNothing() throws Exception {
        raise(hotPlot, IncidentType.HYDRIC_STRESS, IncidentSeverity.CRITICAL);
        evaluate();

        var otherManager = signUp("ROLE_TECHNICAL_MANAGER", "Rosa Condori");
        licenseOf(otherManager);
        authenticateAs(otherManager);
        mvc.perform(get(ALERTS)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        authenticateAs(sharing);
        mvc.perform(get(ALERTS)).andExpect(status().isForbidden());
    }

    private int evaluate() {
        return sectorAlertCommandService.handle(new EvaluateSectorAlertsCommand(
                license.snapshot().id().cooperativeLicenseId())).success().orElseThrow();
    }

    private AgroclimaticIncident raise(String plotId, IncidentType type, IncidentSeverity severity) {
        return incidentRepository.save(AgroclimaticIncident.raise(
                new PlotId(plotId), type, severity,
                new ThresholdBreachInfo("SOIL_MOISTURE", 10.0, 20.0, "%"), List.of("step.one"), Instant.now()));
    }

    private void withdraw(String producerId) {
        var consent = consentRows.findAllByProducerIdOrderByAcceptedAtDesc(UUID.fromString(producerId)).getFirst();
        assertThat(consentCommandService.handle(new WithdrawDataSharingConsentCommand(producerId,
                consent.getId().toString())).isSuccess()).isTrue();
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

package com.arcadiadevs.viora.platform.territory.interfaces.rest;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.orchard.application.commandservices.PlotCommandService;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.RemovePlotCommand;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CooperativeCodeRedemptionCommandService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicenseSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemCooperativeCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.ThresholdBreachInfo;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import com.arcadiadevs.viora.platform.territory.application.commandservices.PlotSectorAssignmentCommandService;
import com.arcadiadevs.viora.platform.territory.application.commandservices.SectorZoneCommandService;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.AssignPlotSectorCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.CreateSectorZoneCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertType;
import com.arcadiadevs.viora.platform.territory.domain.repositories.SectorAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The sector alerts follow the plot events without waiting for the 15-minute scheduler (disabled in the tests):
 * an incident raised or normalized on a member plot, and a plot removed by its owner. The serialization of the
 * evaluations is covered by {@code SectorAlertEvaluatorTest}: H2 does not reproduce the race.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:territoryreactivealerts;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Reactive sector alerts integration")
class ReactiveSectorAlertsIntegrationTest {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration WAIT = Duration.ofSeconds(15);

    @Autowired UserAccountCommandService userAccountCommandService;
    @Autowired CooperativeLicenseRepository licenseRepository;
    @Autowired InvitationCodeRepository invitationCodeRepository;
    @Autowired CooperativeCodeRedemptionCommandService redemptionCommandService;
    @Autowired PlotCommandService plotCommandService;
    @Autowired SectorZoneCommandService sectorZoneCommandService;
    @Autowired PlotSectorAssignmentCommandService plotSectorAssignmentCommandService;
    @Autowired AgroclimaticIncidentRepository incidentRepository;
    @Autowired SectorAlertRepository sectorAlertRepository;
    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired TransactionTemplate transactionTemplate;

    private LicenseId licenseId;
    private String producer;
    private String hotPlot;

    @BeforeEach
    void setUp() {
        var manager = signUp("ROLE_TECHNICAL_MANAGER", "Rubén Ticona");
        var license = licenseOf(manager);
        licenseId = new LicenseId(license.snapshot().id().cooperativeLicenseId());
        producer = member(license, "Teodoro Mamani");
        hotPlot = newPlot(producer, "La Yarada 02", -70.25);
        var coldPlot = newPlot(producer, "Lote Norte", -70.30);
        var zone = sectorZoneCommandService.handle(new CreateSectorZoneCommand(manager, "La Yarada Baja"))
                .success().orElseThrow().snapshot().id().sectorZoneId();
        for (var plot : List.of(hotPlot, coldPlot)) {
            assertThat(plotSectorAssignmentCommandService.handle(
                    new AssignPlotSectorCommand(producer, plot, zone, null)).isSuccess()).isTrue();
        }
    }

    @Test
    @DisplayName("A critical incident raised on a member plot opens the sector alert, and its normalization closes it")
    void incidentsOpenAndNormalizeTheAlert() throws Exception {
        var incident = raise(hotPlot, IncidentType.HYDRIC_STRESS, IncidentSeverity.CRITICAL);

        waitUntil(() -> openAlerts() == 1);
        assertThat(sectorAlertRepository.findOpenByLicenseId(licenseId).getFirst().snapshot().type())
                .isEqualTo(SectorAlertType.SECTOR_CRITICAL);

        normalize(incident);

        waitUntil(() -> openAlerts() == 0);
    }

    @Test
    @DisplayName("Removing the plot that turned the sector critical normalizes its alert")
    void removingThePlotNormalizesTheAlert() throws Exception {
        raise(hotPlot, IncidentType.HYDRIC_STRESS, IncidentSeverity.CRITICAL);
        waitUntil(() -> openAlerts() == 1);

        assertThat(plotCommandService.handle(new RemovePlotCommand(hotPlot, producer, null)).isSuccess()).isTrue();

        waitUntil(() -> openAlerts() == 0);
    }

    private int openAlerts() {
        return sectorAlertRepository.findOpenByLicenseId(licenseId).size();
    }

    /** Raises an incident and publishes its events in one transaction, as the telemetry command service does. */
    private AgroclimaticIncident raise(String plotId, IncidentType type, IncidentSeverity severity) {
        return transactionTemplate.execute(status -> {
            var incident = AgroclimaticIncident.raise(new PlotId(plotId), type, severity,
                    new ThresholdBreachInfo("SOIL_MOISTURE", 10.0, 20.0, "%"), List.of("step.one"), Instant.now());
            var saved = incidentRepository.save(incident);
            publish(incident);
            return saved;
        });
    }

    private void normalize(AgroclimaticIncident incident) {
        transactionTemplate.executeWithoutResult(status -> {
            incident.normalize(Instant.now());
            incidentRepository.save(incident);
            publish(incident);
        });
    }

    private void publish(AgroclimaticIncident incident) {
        for (var event : incident.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        incident.clearDomainEvents();
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        var deadline = Instant.now().plus(WAIT);
        while (!condition.getAsBoolean()) {
            assertThat(Instant.now()).as("condition not reached in %s", WAIT).isBefore(deadline);
            Thread.sleep(100);
        }
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
        var producerId = signUp("ROLE_PRODUCER", fullName);
        var code = randomCode();
        var stored = licenseRepository.findById(owner.snapshot().id()).orElseThrow();
        var issued = stored.issueCode(new CooperativeCode(code).fingerprint(), new HectaresQuota(new BigDecimal("12")),
                Instant.now().plus(Duration.ofDays(7)));
        licenseRepository.save(stored);
        invitationCodeRepository.save(issued);
        var command = new RedeemCooperativeCodeCommand(producerId, code, "2026-11-v1");
        assertThat(redemptionCommandService.handle(command).isSuccess()).isTrue();
        return producerId;
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
}

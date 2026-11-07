package com.arcadiadevs.viora.platform.territory.domain;

import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorAlert;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotRisk;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotRiskAssessment;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskLevel;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskReason;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskReasonType;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertFacts;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertSeverity;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertType;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.services.SectorAlertPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sector alert policy and aggregate unit tests")
class SectorAlertPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-06T15:00:00Z");
    private static final LocalDate TOMORROW = LocalDate.of(2026, 11, 7);

    private final SectorZoneId sector = new SectorZoneId(UUID.randomUUID().toString());

    @Test
    @DisplayName("Overload or a critical incident makes the sector critical, led by the largest overload")
    void criticalSector() {
        var big = plot(RiskLevel.CRITICAL, 43.0, new RiskReason(RiskReasonType.OVERLOAD, null, 43.0, null));
        var small = plot(RiskLevel.CRITICAL, 34.0, new RiskReason(RiskReasonType.OVERLOAD, null, 34.0, null));
        var heat = plot(RiskLevel.CRITICAL, 0.0,
                new RiskReason(RiskReasonType.CRITICAL_INCIDENT, "HEAT_WAVE", null, null));
        var green = plot(RiskLevel.OPTIMAL, 0.0);

        var alerts = SectorAlertPolicy.alertsFor(List.of(small, green, big, heat));

        assertThat(alerts).containsOnlyKeys(SectorAlertType.SECTOR_CRITICAL);
        var facts = alerts.get(SectorAlertType.SECTOR_CRITICAL);
        assertThat(facts.affectedPlots()).isEqualTo(3);
        assertThat(facts.sectorPlots()).isEqualTo(4);
        assertThat(facts.leadPlotId()).isEqualTo(big.plotId());
        assertThat(facts.leadReason()).isEqualTo(RiskReasonType.OVERLOAD);
        assertThat(facts.leadValue()).isEqualTo(43.0);
        assertThat(SectorAlertType.SECTOR_CRITICAL.severity()).isEqualTo(SectorAlertSeverity.CRITICAL);
    }

    @Test
    @DisplayName("Frost alone is an «Atención» frost alert, not a critical sector, led by the coldest forecast")
    void frostIsItsOwnAlert() {
        var mild = plot(RiskLevel.CRITICAL, 0.0,
                new RiskReason(RiskReasonType.FROST_FORECAST, null, 1.4, TOMORROW));
        var cold = plot(RiskLevel.CRITICAL, 0.0,
                new RiskReason(RiskReasonType.FROST_FORECAST, null, 0.8, TOMORROW));
        var incident = plot(RiskLevel.MODERATE, 0.0,
                new RiskReason(RiskReasonType.WARNING_INCIDENT, "FROST_WARNING", null, null));

        var alerts = SectorAlertPolicy.alertsFor(List.of(mild, cold, incident));

        assertThat(alerts).containsOnlyKeys(SectorAlertType.FROST);
        var facts = alerts.get(SectorAlertType.FROST);
        assertThat(facts.affectedPlots()).isEqualTo(3);
        assertThat(facts.leadPlotId()).isEqualTo(cold.plotId());
        assertThat(facts.leadValue()).isEqualTo(0.8);
        assertThat(facts.leadDate()).isEqualTo(TOMORROW);
        assertThat(SectorAlertType.FROST.severity()).isEqualTo(SectorAlertSeverity.ATTENTION);
    }

    @Test
    @DisplayName("A frost incident without forecast still raises the frost alert; a calm sector raises none")
    void frostIncidentAndCalmSector() {
        var incident = plot(RiskLevel.CRITICAL, 0.0,
                new RiskReason(RiskReasonType.CRITICAL_INCIDENT, "FROST_WARNING", null, null));

        assertThat(SectorAlertPolicy.alertsFor(List.of(incident))).containsOnlyKeys(SectorAlertType.FROST);
        assertThat(SectorAlertPolicy.alertsFor(List.of(plot(RiskLevel.OPTIMAL, 0.0),
                plot(RiskLevel.NO_DATA, null)))).isEmpty();
        assertThat(SectorAlertPolicy.alertsFor(List.of())).isEmpty();
    }

    @Test
    @DisplayName("A sector takes the color of its most urgent plot; without plots it has no data")
    void sectorLevel() {
        assertThat(SectorAlertPolicy.levelOf(List.of(plot(RiskLevel.OPTIMAL, 0.0), plot(RiskLevel.MODERATE, 0.0))))
                .isEqualTo(RiskLevel.MODERATE);
        assertThat(SectorAlertPolicy.levelOf(List.of())).isEqualTo(RiskLevel.NO_DATA);
    }

    @Test
    @DisplayName("An alert refreshes while open, normalizes once with the sector's color, and then never changes")
    void alertLifecycle() {
        var facts = new SectorAlertFacts(1, 4, null, null, RiskReasonType.OVERLOAD, 34.0, null);
        var alert = SectorAlert.open(new LicenseId(UUID.randomUUID().toString()), sector,
                SectorAlertType.SECTOR_CRITICAL, facts, NOW);

        assertThat(alert.refresh(facts, NOW.plusSeconds(60))).isFalse();
        var worse = new SectorAlertFacts(2, 4, null, null, RiskReasonType.OVERLOAD, 43.0, null);
        assertThat(alert.refresh(worse, NOW.plusSeconds(60))).isTrue();
        assertThat(alert.snapshot().updatedAt()).isEqualTo(NOW.plusSeconds(60));

        assertThat(alert.normalize(RiskLevel.MODERATE, NOW.plusSeconds(120))).isTrue();
        assertThat(alert.isOpen()).isFalse();
        assertThat(alert.snapshot().levelAtNormalize()).isEqualTo(RiskLevel.MODERATE);
        assertThat(alert.normalize(RiskLevel.OPTIMAL, NOW.plusSeconds(180))).isFalse();
        assertThat(alert.refresh(facts, NOW.plusSeconds(180))).isFalse();
        assertThat(alert.snapshot().facts()).isEqualTo(worse);
    }

    private PlotRisk plot(RiskLevel level, Double overload, RiskReason... reasons) {
        return new PlotRisk(new PlotId(UUID.randomUUID().toString()), new ProducerId(UUID.randomUUID().toString()),
                sector, new PlotRiskAssessment(level, List.of(reasons), overload));
    }
}

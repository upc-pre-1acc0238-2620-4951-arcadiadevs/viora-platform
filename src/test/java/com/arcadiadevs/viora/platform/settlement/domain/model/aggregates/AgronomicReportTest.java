package com.arcadiadevs.viora.platform.settlement.domain.model.aggregates;

import com.arcadiadevs.viora.platform.settlement.domain.model.events.CampaignHarvestSettledEvent;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AgronomicReportTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC);
    private final PlotId plotId = new PlotId(UUID.randomUUID().toString());
    private final UserId producer = new UserId(UUID.randomUUID().toString());

    private HarvestSettlementSnapshot settle(AgronomicReport report, int year, double green, double black) {
        return report.settleCampaign(new CampaignYear(year), new OliveWeight(green), new OliveWeight(black),
                null, null, ThinningBalance.notRecorded(), new TreeMap<>(), CLOCK);
    }

    @Test
    void settlesACampaignWithItsTotalAndPublishesTheEvent() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        var settlement = report.settleCampaign(new CampaignYear(2026), new OliveWeight(8200.0),
                new OliveWeight(6050.0), 105.0, "Weights verified", ThinningBalance.notRecorded(), new TreeMap<>(), CLOCK);
        assertEquals(14250.0, settlement.totalHarvestWeight().kilograms());
        assertEquals(SettlementStatus.SETTLED, settlement.status());
        assertEquals(CLOCK.instant(), settlement.settledAt());
        assertEquals(report.snapshot().id(), settlement.reportId());
        assertEquals(plotId, settlement.plotId());
        assertEquals(StabilizationStatus.INSUFFICIENT_BASELINE, settlement.trendCurve().status());
        var event = (CampaignHarvestSettledEvent) report.domainEvents().iterator().next();
        assertEquals(settlement.id().settlementId(), event.settlementId());
        assertEquals(plotId.plotId(), event.plotId());
        assertEquals(2026, event.campaignYear());
        assertEquals(14250.0, event.totalYieldKg());
        assertEquals(105.0, event.commercialFruitsPerKg());
    }

    @Test
    void rejectsSettlingTheSameCampaignTwiceWithoutChangingIt() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        var first = settle(report, 2026, 8200, 6050);
        assertThrows(IllegalStateException.class, () -> settle(report, 2026, 1, 1));
        assertEquals(1, report.snapshot().settlements().size());
        assertEquals(first, report.settlementOf(new CampaignYear(2026)).orElseThrow());
        assertEquals(1, report.domainEvents().size());
    }

    @Test
    void aLaterCampaignPreservesTheFrozenEarlierOne() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        var first = settle(report, 2026, 7000, 0);
        var second = settle(report, 2027, 0, 5000);
        assertEquals(first, report.settlementOf(new CampaignYear(2026)).orElseThrow());
        assertEquals(1, first.trendCurve().settledCampaigns());
        assertEquals(2, second.trendCurve().settledCampaigns());
        assertEquals(second.trendCurve(), report.trendCurve().orElseThrow());
    }

    @Test
    void freezesTheThinningBalanceOfTheCampaign() {
        var record = new ThinningExecutionRecord(UUID.randomUUID().toString(), UUID.randomUUID().toString(), plotId,
                2026, LocalDate.of(2026, 1, 10), 30.0, 25.0, true);
        var report = AgronomicReport.createForPlot(plotId, producer);
        var settlement = report.settleCampaign(new CampaignYear(2026), new OliveWeight(8200.0), new OliveWeight(0.0),
                null, null, ThinningBalance.of(record), new TreeMap<>(), CLOCK);
        var balance = settlement.thinningBalance();
        assertEquals(ThinningComplianceStatus.EXECUTED_ON_TIME, balance.status());
        assertEquals(30.0, balance.prescribedRemovalPercentage());
        assertEquals(25.0, balance.actualRemovalPercentage());
        assertEquals(-5.0, balance.deviationPercentagePoints());
    }

    @Test
    void classifiesLateAndMissingThinning() {
        var late = new ThinningExecutionRecord("e", UUID.randomUUID().toString(), plotId, 2026,
                LocalDate.of(2026, 2, 1), null, 20.0, false);
        assertEquals(ThinningComplianceStatus.EXECUTED_LATE, ThinningBalance.of(late).status());
        assertNull(ThinningBalance.of(late).deviationPercentagePoints());
        assertEquals(ThinningComplianceStatus.NOT_RECORDED, ThinningBalance.of(null).status());
    }

    @Test
    void rejectsAZeroTotal() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        assertThrows(IllegalArgumentException.class, () -> settle(report, 2026, 0, 0));
        assertTrue(report.snapshot().settlements().isEmpty());
        assertTrue(report.domainEvents().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidWeights(double kilograms) {
        assertThrows(IllegalArgumentException.class, () -> new OliveWeight(kilograms));
    }

    @ParameterizedTest
    @ValueSource(ints = {1999, 2101})
    void rejectsCampaignsOutsideTheSettlementRange(int year) {
        assertThrows(IllegalArgumentException.class, () -> new CampaignYear(year));
    }

    @Test
    void rejectsInvalidCaliberAndOverlongNotes() {
        var report = AgronomicReport.createForPlot(plotId, producer);
        assertThrows(IllegalArgumentException.class, () -> report.settleCampaign(new CampaignYear(2026),
                new OliveWeight(1.0), new OliveWeight(1.0), 0.0, null, ThinningBalance.notRecorded(), new TreeMap<>(), CLOCK));
        assertThrows(IllegalArgumentException.class, () -> report.settleCampaign(new CampaignYear(2026),
                new OliveWeight(1.0), new OliveWeight(1.0), null, "x".repeat(1001), ThinningBalance.notRecorded(),
                new TreeMap<>(), CLOCK));
        assertTrue(report.snapshot().settlements().isEmpty());
    }
}

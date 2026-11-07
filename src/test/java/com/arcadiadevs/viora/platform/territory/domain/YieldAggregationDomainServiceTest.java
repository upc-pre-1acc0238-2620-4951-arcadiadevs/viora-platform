package com.arcadiadevs.viora.platform.territory.domain;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.IntakeStatus;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSettlement;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotYield;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorPlacement;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.services.YieldAggregationDomainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Intake projection unit tests")
class YieldAggregationDomainServiceTest {

    private final SectorPlacement lowerYarada = SectorPlacement.assigned(new SectorZoneId(UUID.randomUUID().toString()));
    private final SectorPlacement magollo = SectorPlacement.assigned(new SectorZoneId(UUID.randomUUID().toString()));

    @Test
    @DisplayName("Only estimable plots add up, without extrapolation, split green and black by each plot's history")
    void addsEstimablePlotsOnly() {
        var intake = YieldAggregationDomainService.project(List.of(
                plotYield(lowerYarada, true, 10_000.0, 0.6),
                plotYield(lowerYarada, true, 5_000.0, null),
                plotYield(magollo, true, null, 0.5),
                plotYield(magollo, false, null, null)));

        assertThat(intake.status()).isEqualTo(IntakeStatus.SUFFICIENT);
        assertThat(intake.totalKg()).isEqualTo(15_000.0);
        assertThat(intake.greenKg()).isCloseTo(6_000.0, within(0.001));
        assertThat(intake.blackKg()).isCloseTo(4_000.0, within(0.001));
        assertThat(intake.unclassifiedKg()).isEqualTo(5_000.0);
        assertThat(intake.plots()).isEqualTo(4);
        assertThat(intake.sampledPlots()).isEqualTo(3);
        assertThat(intake.estimablePlots()).isEqualTo(2);
        assertThat(intake.coveragePercent()).isEqualTo(75.0);
        assertThat(intake.sectors()).hasSize(2);
        assertThat(intake.sectors().getFirst().kilograms()).isEqualTo(15_000.0);
        assertThat(intake.sectors().get(1).kilograms()).isNull();
        assertThat(intake.sectors().get(1).sampledPlots()).isEqualTo(1);
    }

    @Test
    @DisplayName("Below half the plots sampled the projection is preliminary; with none sampled there is no figure")
    void coverageThreshold() {
        var preliminary = YieldAggregationDomainService.project(List.of(plotYield(lowerYarada, true, 1_000.0, null),
                plotYield(lowerYarada, false, null, null), plotYield(magollo, false, null, null)));
        assertThat(preliminary.status()).isEqualTo(IntakeStatus.PRELIMINARY);
        assertThat(preliminary.totalKg()).isEqualTo(1_000.0);

        var half = YieldAggregationDomainService.project(List.of(plotYield(lowerYarada, true, 1_000.0, null),
                plotYield(magollo, false, null, null)));
        assertThat(half.status()).isEqualTo(IntakeStatus.SUFFICIENT);

        var none = YieldAggregationDomainService.project(List.of(plotYield(lowerYarada, false, null, null)));
        assertThat(none.status()).isEqualTo(IntakeStatus.NO_DATA);
        assertThat(none.totalKg()).isNull();
        assertThat(YieldAggregationDomainService.project(List.of()).status()).isEqualTo(IntakeStatus.NO_DATA);
    }

    @Test
    @DisplayName("A past campaign adds up what was settled: closed when every plot settled, partial otherwise")
    void settledCampaigns() {
        var closed = YieldAggregationDomainService.settled(List.of(settled(lowerYarada, 3_000, 1_000),
                settled(magollo, 2_000, 2_000)));
        assertThat(closed.status()).isEqualTo(IntakeStatus.CLOSED);
        assertThat(closed.totalKg()).isEqualTo(8_000.0);
        assertThat(closed.greenKg()).isEqualTo(5_000.0);
        assertThat(closed.unclassifiedKg()).isZero();

        var partial = YieldAggregationDomainService.settled(List.of(settled(lowerYarada, 3_000, 1_000),
                new PlotSettlement(plot(), magollo, false, 0, 0)));
        assertThat(partial.status()).isEqualTo(IntakeStatus.PARTIAL);
        assertThat(partial.coveragePercent()).isEqualTo(50.0);

        var none = YieldAggregationDomainService.settled(List.of(new PlotSettlement(plot(), magollo, false, 0, 0)));
        assertThat(none.status()).isEqualTo(IntakeStatus.NO_DATA);
        assertThat(none.totalKg()).isNull();
    }

    @Test
    @DisplayName("Plots «Por confirmar» and without a sector are counted in their own groups")
    void ungroupedPlots() {
        var intake = YieldAggregationDomainService.project(List.of(plotYield(SectorPlacement.PENDING, true, 500.0, null),
                plotYield(SectorPlacement.NONE, false, null, null)));

        assertThat(intake.sectors()).extracting(sector -> sector.sector().status())
                .containsExactly("PENDING", "NONE");
    }

    private static PlotYield plotYield(SectorPlacement sector, boolean sampled, Double kilograms, Double greenShare) {
        return new PlotYield(plot(), sector, sampled, kilograms, greenShare);
    }

    private static PlotSettlement settled(SectorPlacement sector, double green, double black) {
        return new PlotSettlement(plot(), sector, true, green, black);
    }

    private static PlotId plot() {
        return new PlotId(UUID.randomUUID().toString());
    }
}

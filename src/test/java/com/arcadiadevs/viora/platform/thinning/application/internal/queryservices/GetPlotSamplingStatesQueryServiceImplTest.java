package com.arcadiadevs.viora.platform.thinning.application.internal.queryservices;

import com.arcadiadevs.viora.platform.thinning.PrescriptionTestData;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetPlotSamplingStatesQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPlotSamplingStatesQueryService Unit Tests")
class GetPlotSamplingStatesQueryServiceImplTest {

    @Mock
    private FruitThinningPrescriptionRepository prescriptionRepository;

    @Mock
    private ExternalOrchardService externalOrchardService;

    private GetPlotSamplingStatesQueryServiceImpl queryService;

    private final String producerId = "550e8400-e29b-41d4-a716-446655440000";
    private final CampaignYear campaignYear = new CampaignYear(2026);

    @BeforeEach
    void setUp() {
        queryService = new GetPlotSamplingStatesQueryServiceImpl(
                prescriptionRepository,
                externalOrchardService
        );
    }

    @Test
    @DisplayName("Should return empty list when producer has no active plots")
    void shouldReturnEmptyListWhenNoActivePlots() {
        when(externalOrchardService.findActivePlotIdsByProducerId(producerId))
                .thenReturn(List.of());

        var query = new GetPlotSamplingStatesQuery(producerId, campaignYear);
        var result = queryService.handle(query);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success().orElseThrow()).isEmpty();
        verify(prescriptionRepository, never()).findByPlotIdInAndCampaignYear(any(), any());
    }

    @Test
    @DisplayName("Should categorize plot states as NOT_STARTED, IN_PROGRESS, and COMPLETED")
    void shouldReturnPlotsWithAccurateSamplingStates() {
        var plot1 = new PlotId("550e8400-e29b-41d4-a716-446655440001");
        var plot2 = new PlotId("550e8400-e29b-41d4-a716-446655440002");
        var plot3 = new PlotId("550e8400-e29b-41d4-a716-446655440003");

        when(externalOrchardService.findActivePlotIdsByProducerId(producerId))
                .thenReturn(List.of(plot1, plot2, plot3));

        when(externalOrchardService.findPlotName(plot1)).thenReturn(Optional.of("Plot Alpha"));
        when(externalOrchardService.findPlotVariety(plot1)).thenReturn(Optional.of("Sevillana"));
        when(externalOrchardService.findPlotAreaHectares(plot1)).thenReturn(Optional.of(5.0));

        when(externalOrchardService.findPlotName(plot2)).thenReturn(Optional.of("Plot Beta"));
        when(externalOrchardService.findPlotVariety(plot2)).thenReturn(Optional.of("Arbequina"));
        when(externalOrchardService.findPlotAreaHectares(plot2)).thenReturn(Optional.of(3.2));

        when(externalOrchardService.findPlotName(plot3)).thenReturn(Optional.of("Plot Gamma"));
        when(externalOrchardService.findPlotVariety(plot3)).thenReturn(Optional.of("Criolla"));
        when(externalOrchardService.findPlotAreaHectares(plot3)).thenReturn(Optional.of(1.8));

        // Plot 1: Representative (5 trees) -> COMPLETED (since representative is >= 5 trees in domain rule)
        var p1 = PrescriptionTestData.representative(plot1);

        // Plot 2: 2 trees -> IN_PROGRESS
        var p2 = PrescriptionTestData.sampling(plot2);
        var twoRecords = List.of(
                TreeSamplingRecord.create("T-1", 20, 10, 120.0, LocalDate.now()),
                TreeSamplingRecord.create("T-2", 20, 10, 120.0, LocalDate.now())
        );
        p2.ingestSamplingsBatch(new UserId(producerId), new SamplingBatchId("batch-2"), twoRecords);

        // Plot 3: No prescription -> NOT_STARTED

        when(prescriptionRepository.findByPlotIdInAndCampaignYear(List.of(plot1, plot2, plot3), campaignYear))
                .thenReturn(List.of(p1, p2));

        var query = new GetPlotSamplingStatesQuery(producerId, campaignYear);
        var result = queryService.handle(query);

        assertThat(result.isSuccess()).isTrue();
        var states = result.success().orElseThrow();
        assertThat(states).hasSize(3);

        // Plot 1: COMPLETED
        var state1 = states.stream().filter(s -> s.plotId().equals(plot1)).findFirst().orElseThrow();
        assertThat(state1.plotName()).isEqualTo("Plot Alpha");
        assertThat(state1.variety()).isEqualTo("Sevillana");
        assertThat(state1.samplingStatus()).isEqualTo("COMPLETED");
        assertThat(state1.sampledTreesCount()).isEqualTo(5);
        assertThat(state1.treesNeeded()).isEqualTo(0);
        assertThat(state1.isRepresentative()).isTrue();
        assertThat(state1.areaHectares()).isEqualTo(5.0);

        // Plot 2: IN_PROGRESS
        var state2 = states.stream().filter(s -> s.plotId().equals(plot2)).findFirst().orElseThrow();
        assertThat(state2.plotName()).isEqualTo("Plot Beta");
        assertThat(state2.variety()).isEqualTo("Arbequina");
        assertThat(state2.samplingStatus()).isEqualTo("IN_PROGRESS");
        assertThat(state2.sampledTreesCount()).isEqualTo(2);
        assertThat(state2.treesNeeded()).isEqualTo(3);
        assertThat(state2.isRepresentative()).isFalse();
        assertThat(state2.areaHectares()).isEqualTo(3.2);

        // Plot 3: NOT_STARTED
        var state3 = states.stream().filter(s -> s.plotId().equals(plot3)).findFirst().orElseThrow();
        assertThat(state3.plotName()).isEqualTo("Plot Gamma");
        assertThat(state3.variety()).isEqualTo("Criolla");
        assertThat(state3.samplingStatus()).isEqualTo("NOT_STARTED");
        assertThat(state3.sampledTreesCount()).isEqualTo(0);
        assertThat(state3.treesNeeded()).isEqualTo(5);
        assertThat(state3.isRepresentative()).isFalse();
        assertThat(state3.areaHectares()).isEqualTo(1.8);
    }
}

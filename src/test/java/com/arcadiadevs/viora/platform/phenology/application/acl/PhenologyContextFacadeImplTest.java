package com.arcadiadevs.viora.platform.phenology.application.acl;

import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather.HourlyTemperatureProvider;
import com.arcadiadevs.viora.platform.phenology.application.queryservices.HarvestRecordQueryService;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTrackerSnapshot;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.HistoricalHarvestEntrySnapshot;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetChillAccumulationTrackerByPlotIdQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.BiennialBearingIndex;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotBearingIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PhenologyContextFacadeImpl biennial bearing index")
class PhenologyContextFacadeImplTest {

    @Mock
    private HarvestRecordQueryService harvestRecordQueryService;
    @Mock
    private ExternalOrchardService externalOrchardService;
    @Mock
    private HourlyTemperatureProvider temperatureProvider;

    private PhenologyContextFacadeImpl facade;
    private final String plotId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        facade = new PhenologyContextFacadeImpl(harvestRecordQueryService, externalOrchardService, temperatureProvider);
    }

    @Test
    @DisplayName("Returns the tracker BBI with its alternation band")
    void returnsTheBbiWithItsBand() {
        givenTracker(0.51, 3);

        assertThat(facade.findBiennialBearingIndex(plotId))
                .contains(new PlotBearingIndex(0.51, "SEVERE_ALTERNATION"));
    }

    @Test
    @DisplayName("Returns nothing while the history is too short to classify the index")
    void returnsNothingWithInsufficientHistory() {
        givenTracker(0.0, 1);

        assertThat(facade.findBiennialBearingIndex(plotId)).isEmpty();
    }

    @Test
    @DisplayName("Returns nothing when the plot has no tracker")
    void returnsNothingWithoutTracker() {
        when(harvestRecordQueryService.handle(any(GetChillAccumulationTrackerByPlotIdQuery.class)))
                .thenReturn(Optional.empty());

        assertThat(facade.findBiennialBearingIndex(plotId)).isEmpty();
    }

    @Test
    @DisplayName("Returns nothing for a malformed or missing plot id without querying")
    void returnsNothingForAMalformedPlotId() {
        assertThat(facade.findBiennialBearingIndex("not-a-uuid")).isEmpty();
        assertThat(facade.findBiennialBearingIndex(null)).isEmpty();
        verify(harvestRecordQueryService, never()).handle(any(GetChillAccumulationTrackerByPlotIdQuery.class));
    }

    private void givenTracker(double bbi, int harvests) {
        var tracker = mock(ChillAccumulationTracker.class);
        List<HistoricalHarvestEntrySnapshot> history = java.util.stream.IntStream.range(0, harvests)
                .mapToObj(i -> mock(HistoricalHarvestEntrySnapshot.class))
                .toList();
        when(tracker.snapshot()).thenReturn(new ChillAccumulationTrackerSnapshot(
                null, null, null, history, new BiennialBearingIndex(bbi), 0L));
        when(harvestRecordQueryService.handle(any(GetChillAccumulationTrackerByPlotIdQuery.class)))
                .thenReturn(Optional.of(tracker));
    }
}

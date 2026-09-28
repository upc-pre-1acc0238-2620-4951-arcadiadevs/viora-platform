package com.arcadiadevs.viora.platform.phenology.application;

import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.application.internal.queryservices.HarvestRecordQueryServiceImpl;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetHarvestRecordsByPlotIdQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestYield;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HarvestRecordQueryService Unit Tests")
class HarvestRecordQueryServiceImplTest {

    @Mock
    private ChillAccumulationTrackerRepository trackerRepository;

    @Mock
    private ExternalOrchardService externalOrchardService;

    private HarvestRecordQueryServiceImpl queryService;

    private final String validPlotId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        queryService = new HarvestRecordQueryServiceImpl(trackerRepository, externalOrchardService);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when plotId format is invalid")
    void shouldThrowWhenPlotIdIsInvalid() {
        assertThatThrownBy(() -> new GetHarvestRecordsByPlotIdQuery("invalid-uuid", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.id.invalid_uuid");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when campaignYear is out of allowed range")
    void shouldThrowWhenCampaignYearIsOutOfRange() {
        assertThatThrownBy(() -> new GetHarvestRecordsByPlotIdQuery(validPlotId, 1950))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phenology.campaign_year.invalid");
    }

    @Test
    @DisplayName("Should return not found when plot does not exist or is not active in ACL")
    void shouldReturnNotFoundWhenPlotDoesNotExistInAcl() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(false);

        var query = new GetHarvestRecordsByPlotIdQuery(validPlotId, null);
        var result = queryService.handle(query);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure()).isPresent();
        assertThat(result.failure().get().code()).isEqualTo("PLOT_NOT_FOUND");
    }

    @Test
    @DisplayName("Should return empty list when tracker does not exist for plot")
    void shouldReturnEmptyListWhenTrackerNotFound() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.empty());

        var query = new GetHarvestRecordsByPlotIdQuery(validPlotId, null);
        var result = queryService.handle(query);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success()).isPresent();
        assertThat(result.success().get()).isEmpty();
    }

    @Test
    @DisplayName("Should return all harvest records when no campaignYear filter is specified")
    void shouldReturnAllHarvestRecordsWhenNoFilter() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);

        var tracker = ChillAccumulationTracker.create(new PlotId(validPlotId), new CampaignYear(2023));
        tracker.recordHarvest(new CampaignYear(2023), new HarvestYield(10000.0, 6000.0, 4000.0));
        tracker.recordHarvest(new CampaignYear(2024), new HarvestYield(14000.0, 8000.0, 6000.0));

        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.of(tracker));

        var query = new GetHarvestRecordsByPlotIdQuery(validPlotId, null);
        var result = queryService.handle(query);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success()).isPresent();
        assertThat(result.success().get()).hasSize(2);
    }

    @Test
    @DisplayName("Should return only filtered harvest records when campaignYear filter is specified")
    void shouldReturnFilteredHarvestRecordsWhenFilterSpecified() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);

        var tracker = ChillAccumulationTracker.create(new PlotId(validPlotId), new CampaignYear(2023));
        tracker.recordHarvest(new CampaignYear(2023), new HarvestYield(10000.0, 6000.0, 4000.0));
        tracker.recordHarvest(new CampaignYear(2024), new HarvestYield(14000.0, 8000.0, 6000.0));

        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.of(tracker));

        var query = new GetHarvestRecordsByPlotIdQuery(validPlotId, 2024);
        var result = queryService.handle(query);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success()).isPresent();
        assertThat(result.success().get()).hasSize(1);
        assertThat(result.success().get().get(0).campaignYear().value()).isEqualTo(2024);
    }
}

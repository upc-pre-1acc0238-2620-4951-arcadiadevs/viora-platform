package com.arcadiadevs.viora.platform.phenology.application;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.application.internal.queryservices.PhenologyMetricQueryServiceImpl;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetPlotMetricsQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.DynamicErezPortion;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestYield;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricEvaluationResult;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricType;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PhenologyMetricQueryService Application Unit Tests")
class PhenologyMetricQueryServiceTest {

    @Mock
    private ChillAccumulationTrackerRepository trackerRepository;

    @Mock
    private ExternalOrchardService externalOrchardService;

    private PhenologyMetricQueryServiceImpl queryService;

    private final String validPlotId = UUID.randomUUID().toString();
    private final PlotId plotId = new PlotId(validPlotId);

    @BeforeEach
    void setUp() {
        queryService = new PhenologyMetricQueryServiceImpl(trackerRepository, externalOrchardService);
    }

    @Test
    @DisplayName("Should return 404 NOT_FOUND when plot is inactive or not found in Orchard ACL")
    void shouldReturnNotFoundWhenPlotInactiveInAcl() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(false);

        var query = new GetPlotMetricsQuery(plotId, (MetricType) null);
        var result = queryService.handle(query);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure()).isPresent();
        assertThat(result.failure().get().code()).isEqualTo("PLOT_NOT_FOUND");
    }

    @Test
    @DisplayName("Should return 404 NOT_FOUND when tracker aggregate is not initialized for the plot")
    void shouldReturnNotFoundWhenTrackerMissing() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.empty());

        var query = new GetPlotMetricsQuery(plotId, (MetricType) null);
        var result = queryService.handle(query);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure()).isPresent();
        assertThat(result.failure().get().code()).isEqualTo("CHILLACCUMULATIONTRACKER_NOT_FOUND");
    }

    @Test
    @DisplayName("Should return both BBI and Erez chilling metrics when metricType is null")
    void shouldReturnBothMetricsWhenNoFilterSpecified() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);

        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2022));
        tracker.recordHarvest(new CampaignYear(2022), HarvestYield.ofTotal(10000.0));
        tracker.recordHarvest(new CampaignYear(2023), HarvestYield.ofTotal(5000.0));

        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.of(tracker));

        var query = new GetPlotMetricsQuery(plotId, (MetricType) null);
        var result = queryService.handle(query);

        assertThat(result).isInstanceOf(Result.Success.class);
        var success = (Result.Success<List<MetricEvaluationResult>, ApplicationError>) result;
        assertThat(success.value()).hasSize(2);

        var bbiMetric = success.value().stream()
                .filter(m -> m.metricType() == MetricType.BIENNIAL_BEARING_INDEX)
                .findFirst()
                .orElseThrow();
        assertThat(bbiMetric.value()).isEqualTo(0.333);
        assertThat(bbiMetric.qualitativeCategory()).isEqualTo("MODERATE_ALTERNATION");

        var chillingMetric = success.value().stream()
                .filter(m -> m.metricType() == MetricType.EREZ_CHILLING_PORTIONS)
                .findFirst()
                .orElseThrow();
        assertThat(chillingMetric.value()).isEqualTo(28.5);
        assertThat(chillingMetric.qualitativeCategory()).isEqualTo("SATISFIED");
    }

    @Test
    @DisplayName("Should filter and return only BBI when metricType is BIENNIAL_BEARING_INDEX")
    void shouldReturnOnlyBbiWhenFiltered() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);

        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2023));
        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.of(tracker));

        var query = new GetPlotMetricsQuery(plotId, MetricType.BIENNIAL_BEARING_INDEX);
        var result = queryService.handle(query);

        assertThat(result).isInstanceOf(Result.Success.class);
        var success = (Result.Success<List<MetricEvaluationResult>, ApplicationError>) result;
        assertThat(success.value()).hasSize(1);
        assertThat(success.value().get(0).metricType()).isEqualTo(MetricType.BIENNIAL_BEARING_INDEX);
    }

    @Test
    @DisplayName("Should filter and return only chilling portions when metricType is EREZ_CHILLING_PORTIONS")
    void shouldReturnOnlyChillingWhenFiltered() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);

        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2023));
        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.of(tracker));

        var query = new GetPlotMetricsQuery(plotId, MetricType.EREZ_CHILLING_PORTIONS);
        var result = queryService.handle(query);

        assertThat(result).isInstanceOf(Result.Success.class);
        var success = (Result.Success<List<MetricEvaluationResult>, ApplicationError>) result;
        assertThat(success.value()).hasSize(1);
        assertThat(success.value().get(0).metricType()).isEqualTo(MetricType.EREZ_CHILLING_PORTIONS);
    }
}

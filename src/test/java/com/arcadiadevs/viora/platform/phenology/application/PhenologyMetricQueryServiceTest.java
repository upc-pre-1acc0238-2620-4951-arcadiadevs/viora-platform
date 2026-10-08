package com.arcadiadevs.viora.platform.phenology.application;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather.HourlyTemperatureProvider;
import com.arcadiadevs.viora.platform.phenology.application.internal.queryservices.PhenologyMetricQueryServiceImpl;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetPlotMetricsQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestYield;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HourlyTemperature;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricEvaluationResult;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricType;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PhenologyMetricQueryService Application Unit Tests")
class PhenologyMetricQueryServiceTest {

    /** 2026-07-01 at 10:00 in Lima: the winter of 2026 is in progress, June is over. */
    private static final Clock IN_SEASON = Clock.fixed(Instant.parse("2026-07-01T15:00:00Z"), ZoneOffset.UTC);
    /** 2026-10-07 at 10:00 in Lima: off season, the metric describes the winter of 2026. */
    private static final Clock OFF_SEASON = Clock.fixed(Instant.parse("2026-10-07T15:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ChillAccumulationTrackerRepository trackerRepository;

    @Mock
    private ExternalOrchardService externalOrchardService;

    @Mock
    private HourlyTemperatureProvider temperatureProvider;

    private final PlotId plotId = new PlotId(UUID.randomUUID().toString());

    private PhenologyMetricQueryServiceImpl service(Clock clock) {
        return new PhenologyMetricQueryServiceImpl(trackerRepository, externalOrchardService, temperatureProvider, clock);
    }

    @Test
    @DisplayName("Should return 404 NOT_FOUND when plot is inactive or not found in Orchard ACL")
    void shouldReturnNotFoundWhenPlotInactiveInAcl() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(false);

        var result = service(IN_SEASON).handle(new GetPlotMetricsQuery(plotId, (MetricType) null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().get().code()).isEqualTo("PLOT_NOT_FOUND");
    }

    @Test
    @DisplayName("Should return 404 when only the BBI is asked and the plot has no harvest history")
    void shouldReturnNotFoundForBbiWithoutTracker() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.empty());

        var result = service(IN_SEASON).handle(new GetPlotMetricsQuery(plotId, MetricType.BIENNIAL_BEARING_INDEX));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().get().code()).isEqualTo("CHILLACCUMULATIONTRACKER_NOT_FOUND");
    }

    @Test
    @DisplayName("Should compute the chill of a plot without harvest records and skip the BBI")
    void shouldComputeChillWithoutTracker() {
        givenPlotWithWeather();
        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.empty());

        var metrics = success(service(IN_SEASON).handle(new GetPlotMetricsQuery(plotId, (MetricType) null)));

        assertThat(metrics).hasSize(1);
        assertThat(metrics.getFirst().metricType()).isEqualTo(MetricType.EREZ_CHILLING_PORTIONS);
    }

    @Test
    @DisplayName("Should return both BBI and chill when metricType is null and the plot has history")
    void shouldReturnBothMetricsWhenNoFilterSpecified() {
        givenPlotWithWeather();
        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2022));
        tracker.recordHarvest(new CampaignYear(2022), HarvestYield.ofTotal(10000.0));
        tracker.recordHarvest(new CampaignYear(2023), HarvestYield.ofTotal(5000.0));
        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.of(tracker));

        var metrics = success(service(IN_SEASON).handle(new GetPlotMetricsQuery(plotId, (MetricType) null)));

        assertThat(metrics).hasSize(2);
        var bbi = metrics.getFirst();
        assertThat(bbi.metricType()).isEqualTo(MetricType.BIENNIAL_BEARING_INDEX);
        assertThat(bbi.value()).isEqualTo(0.333);
        assertThat(bbi.qualitativeCategory()).isEqualTo("MODERATE_ALTERNATION");
        assertThat(metrics.get(1).metricType()).isEqualTo(MetricType.EREZ_CHILLING_PORTIONS);
    }

    @Test
    @DisplayName("Should evaluate the winter in progress from the real hours, compared with the previous winter")
    void shouldEvaluateTheSeasonInProgress() {
        givenPlotWithWeather();

        var chill = success(service(IN_SEASON).handle(new GetPlotMetricsQuery(plotId, MetricType.EREZ_CHILLING_PORTIONS))).getFirst();

        assertThat(chill.value()).isCloseTo(24.43, offset(0.01));
        assertThat(chill.qualitativeCategory()).isEqualTo("DEFICIENT");
        var details = chill.details();
        assertThat(details).containsEntry("thresholdTarget", 30.0);
        assertThat(details).containsEntry("seasonYear", 2026);
        assertThat(details).containsEntry("seasonStart", "2026-06-01");
        assertThat(details).containsEntry("seasonEnd", "2026-08-31");
        assertThat(details).containsEntry("evaluatedThrough", "2026-06-30");
        assertThat(details).containsEntry("seasonState", "IN_PROGRESS");
        assertThat(details).containsEntry("completionDate", null);
        assertThat(details).containsEntry("projectionStatus", "PROJECTED");
        assertThat(details).containsEntry("projectedCompletionDate", "2026-07-07");
        assertThat(details).containsEntry("thermalAnomaly", "NONE");
        assertThat(details).containsEntry("daysAbove24Celsius", 0);

        @SuppressWarnings("unchecked")
        var previous = (Map<String, Object>) details.get("previousSeason");
        assertThat(previous).containsEntry("seasonYear", 2025);
        assertThat(previous).containsEntry("accumulatedPortions", 0.0);
        assertThat(previous).containsEntry("completionDate", null);

        @SuppressWarnings("unchecked")
        var curve = (List<Map<String, Object>>) details.get("dailyCurve");
        assertThat(curve).hasSize(92);
        assertThat(curve.getFirst()).containsEntry("date", "2026-06-01");
        assertThat((Double) curve.get(29).get("portions")).isCloseTo(24.43, offset(0.01));
        assertThat(curve.get(30)).containsEntry("portions", null);
        assertThat(curve.get(30)).containsEntry("previousSeasonPortions", 0.0);
    }

    @Test
    @DisplayName("Should ask only for whole past days and the full previous winter")
    void shouldAskForTheRightRanges() {
        givenPlotWithWeather();

        service(IN_SEASON).handle(new GetPlotMetricsQuery(plotId, MetricType.EREZ_CHILLING_PORTIONS));

        verify(temperatureProvider).fetchHourlyTemperatures(-18.0, -70.25, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        verify(temperatureProvider).fetchHourlyTemperatures(-18.0, -70.25, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 8, 31));
    }

    @Test
    @DisplayName("Should describe the finished winter when today is outside the season")
    void shouldDescribeTheLastWinterOffSeason() {
        givenPlotWithWeather();

        var chill = success(service(OFF_SEASON).handle(new GetPlotMetricsQuery(plotId, MetricType.EREZ_CHILLING_PORTIONS))).getFirst();

        assertThat(chill.details()).containsEntry("seasonState", "OFF_SEASON");
        assertThat(chill.details()).containsEntry("seasonYear", 2026);
        assertThat(chill.details()).containsEntry("evaluatedThrough", "2026-08-31");
        assertThat(chill.details()).containsEntry("projectionStatus", "NOT_APPLICABLE");
        verify(temperatureProvider).fetchHourlyTemperatures(-18.0, -70.25, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31));
    }

    @Test
    @DisplayName("Should return 503 instead of inventing a value when the weather cannot be read")
    void shouldFailWhenWeatherIsUnreachable() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        when(externalOrchardService.findPlotCentroid(any(PlotId.class))).thenReturn(Optional.of(new double[]{-18.0, -70.25}));
        when(temperatureProvider.fetchHourlyTemperatures(anyDouble(), anyDouble(), any(), any())).thenReturn(Optional.empty());

        var result = service(IN_SEASON).handle(new GetPlotMetricsQuery(plotId, MetricType.EREZ_CHILLING_PORTIONS));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().get().code()).isEqualTo("CHILL_WEATHER_UNAVAILABLE");
    }

    @Test
    @DisplayName("Should keep the current winter when only the previous one cannot be read")
    void shouldTolerateAMissingPreviousWinter() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        when(externalOrchardService.findPlotCentroid(any(PlotId.class))).thenReturn(Optional.of(new double[]{-18.0, -70.25}));
        when(temperatureProvider.fetchHourlyTemperatures(anyDouble(), anyDouble(), eq(LocalDate.of(2026, 6, 1)), any()))
                .thenReturn(Optional.of(days(LocalDate.of(2026, 6, 1), 30, 6.0)));
        when(temperatureProvider.fetchHourlyTemperatures(anyDouble(), anyDouble(), eq(LocalDate.of(2025, 6, 1)), any()))
                .thenReturn(Optional.empty());

        var chill = success(service(IN_SEASON).handle(new GetPlotMetricsQuery(plotId, MetricType.EREZ_CHILLING_PORTIONS))).getFirst();

        assertThat(chill.value()).isCloseTo(24.43, offset(0.01));
        assertThat(chill.details()).containsEntry("previousSeason", null);
    }

    @Test
    @DisplayName("Should not read the weather when only the BBI is asked")
    void shouldNotReadWeatherForBbi() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        when(trackerRepository.findByPlotId(any(PlotId.class)))
                .thenReturn(Optional.of(ChillAccumulationTracker.create(plotId, new CampaignYear(2023))));

        var metrics = success(service(IN_SEASON).handle(new GetPlotMetricsQuery(plotId, MetricType.BIENNIAL_BEARING_INDEX)));

        assertThat(metrics).hasSize(1);
        verify(temperatureProvider, never()).fetchHourlyTemperatures(anyDouble(), anyDouble(), any(), any());
    }

    /** June 2026 is cold (6 °C), the winter of 2025 was mild (15 °C) and fixed no portion. */
    private void givenPlotWithWeather() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        when(externalOrchardService.findPlotCentroid(any(PlotId.class))).thenReturn(Optional.of(new double[]{-18.0, -70.25}));
        when(temperatureProvider.fetchHourlyTemperatures(anyDouble(), anyDouble(), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(2);
                    LocalDate to = invocation.getArgument(3);
                    double celsius = from.getYear() == 2026 ? 6.0 : 15.0;
                    int count = (int) (to.toEpochDay() - from.toEpochDay()) + 1;
                    return Optional.of(days(from, count, celsius));
                });
    }

    private static List<MetricEvaluationResult> success(Result<List<MetricEvaluationResult>, ApplicationError> result) {
        assertThat(result).isInstanceOf(Result.Success.class);
        return ((Result.Success<List<MetricEvaluationResult>, ApplicationError>) result).value();
    }

    private static List<HourlyTemperature> days(LocalDate from, int count, double celsius) {
        var hours = new ArrayList<HourlyTemperature>();
        for (int day = 0; day < count; day++) {
            for (int hour = 0; hour < 24; hour++) {
                hours.add(new HourlyTemperature(from.plusDays(day).atTime(hour, 0), celsius));
            }
        }
        return hours;
    }
}

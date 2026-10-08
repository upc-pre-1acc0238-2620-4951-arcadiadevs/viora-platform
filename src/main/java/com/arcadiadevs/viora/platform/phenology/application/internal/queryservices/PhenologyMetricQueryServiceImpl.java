package com.arcadiadevs.viora.platform.phenology.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather.HourlyTemperatureProvider;
import com.arcadiadevs.viora.platform.phenology.application.queryservices.PhenologyMetricQueryService;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetPlotMetricsQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ChillSeasonEvaluation;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HourlyTemperature;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricEvaluationResult;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricType;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
import com.arcadiadevs.viora.platform.phenology.domain.services.ChillSeasonEvaluator;
import com.arcadiadevs.viora.platform.phenology.domain.services.ErezDynamicModelCalculator;
import com.arcadiadevs.viora.platform.phenology.domain.services.HoblynBbiCalculatorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Application query service orchestrating the evaluation and retrieval of phenological metrics.
 *
 * <p>The Hoblyn BBI comes from the harvest history kept in the plot's tracker. The Erez chill portions are
 * computed from the hourly temperatures observed at the plot centroid, so they do not need any harvest record:
 * the current winter (or the last one, outside June 1 - August 31) is evaluated and compared with the winter
 * before it.</p>
 */
@Service
@Transactional(readOnly = true)
public class PhenologyMetricQueryServiceImpl implements PhenologyMetricQueryService {

    static final String CHILL_MODEL = "Dynamic Model (Fishman & Erez)";
    static final String WEATHER_SOURCE = "Open-Meteo historical weather at the plot centroid";

    private final ChillAccumulationTrackerRepository trackerRepository;
    private final ExternalOrchardService externalOrchardService;
    private final HourlyTemperatureProvider temperatureProvider;
    private final Clock clock;

    /**
     * Constructs the query service injecting dependencies.
     *
     * @param trackerRepository      the domain repository port
     * @param externalOrchardService the outbound ACL service for orchard verifications
     * @param temperatureProvider    the outbound port for the hourly temperatures of the plot
     * @param clock                  the application clock
     */
    public PhenologyMetricQueryServiceImpl(
            ChillAccumulationTrackerRepository trackerRepository,
            ExternalOrchardService externalOrchardService,
            HourlyTemperatureProvider temperatureProvider,
            Clock clock
    ) {
        this.trackerRepository = trackerRepository;
        this.externalOrchardService = externalOrchardService;
        this.temperatureProvider = temperatureProvider;
        this.clock = clock;
    }

    @Override
    public Result<List<MetricEvaluationResult>, ApplicationError> handle(GetPlotMetricsQuery query) {
        var plotId = query.plotId();
        if (!externalOrchardService.existsActivePlot(plotId)) {
            return Result.failure(ApplicationError.notFound("Plot", plotId.plotId()));
        }

        boolean wantsBbi = query.metricType() == null || query.metricType() == MetricType.BIENNIAL_BEARING_INDEX;
        boolean wantsChill = query.metricType() == null || query.metricType() == MetricType.EREZ_CHILLING_PORTIONS;
        List<MetricEvaluationResult> metrics = new ArrayList<>();

        if (wantsBbi) {
            var bbi = evaluateBbi(plotId);
            if (bbi.isPresent()) {
                metrics.add(bbi.get());
            } else if (!wantsChill) {
                return Result.failure(ApplicationError.notFound("ChillAccumulationTracker", plotId.plotId()));
            }
        }

        if (wantsChill) {
            var chill = evaluateChill(plotId);
            if (chill.isFailure()) {
                return Result.failure(chill.failure().orElseThrow());
            }
            metrics.add(((Result.Success<MetricEvaluationResult, ApplicationError>) chill).value());
        }

        return Result.success(metrics);
    }

    private Optional<MetricEvaluationResult> evaluateBbi(PlotId plotId) {
        return trackerRepository.findByPlotId(plotId).map(tracker -> {
            var snapshot = tracker.snapshot();
            int evaluatedYearsCount = snapshot.harvestHistory().size();
            var evaluatedBbi = snapshot.calculatedBbi();

            Map<String, Object> bbiDetails = new LinkedHashMap<>();
            bbiDetails.put("formula", "Hoblyn (1936)");
            bbiDetails.put("evaluatedYearsCount", evaluatedYearsCount);
            bbiDetails.put("sampleSufficiency", HoblynBbiCalculatorService.evaluateSufficiency(evaluatedYearsCount).name());

            return new MetricEvaluationResult(
                    MetricType.BIENNIAL_BEARING_INDEX,
                    evaluatedBbi.value(),
                    HoblynBbiCalculatorService.classifyAlternation(evaluatedBbi, evaluatedYearsCount).name(),
                    bbiDetails,
                    clock.instant()
            );
        });
    }

    private Result<MetricEvaluationResult, ApplicationError> evaluateChill(PlotId plotId) {
        var centroid = externalOrchardService.findPlotCentroid(plotId);
        if (centroid.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Plot", plotId.plotId()));
        }
        double latitude = centroid.get()[0];
        double longitude = centroid.get()[1];

        var today = LocalDate.now(clock.withZone(HourlyTemperatureProvider.PLOT_TIME_ZONE));
        int seasonYear = ChillSeasonEvaluator.seasonYearFor(today);
        double threshold = ErezDynamicModelCalculator.DEFAULT_VARIETAL_CHILL_THRESHOLD;

        var currentHours = seasonHours(latitude, longitude, seasonYear, today);
        if (currentHours.isEmpty()) {
            return Result.failure(ApplicationError.unavailable("chill-weather", plotId.plotId()));
        }
        var current = ChillSeasonEvaluator.evaluate(seasonYear, currentHours.get(), threshold, today);

        var previousEnd = ChillSeasonEvaluator.seasonEnd(seasonYear - 1);
        var previous = seasonHours(latitude, longitude, seasonYear - 1, today)
                .map(hours -> ChillSeasonEvaluator.evaluate(seasonYear - 1, hours, threshold, previousEnd.plusDays(1)))
                .orElse(null);

        return Result.success(new MetricEvaluationResult(
                MetricType.EREZ_CHILLING_PORTIONS,
                current.accumulatedPortions(),
                ErezDynamicModelCalculator.evaluateSatisfactionStatus(current.accumulatedPortions(), threshold),
                chillDetails(current, previous),
                clock.instant()
        ));
    }

    private Optional<List<HourlyTemperature>> seasonHours(double latitude, double longitude, int seasonYear, LocalDate today) {
        var from = ChillSeasonEvaluator.seasonStart(seasonYear);
        var seasonEnd = ChillSeasonEvaluator.seasonEnd(seasonYear);
        var yesterday = today.minusDays(1);
        var to = yesterday.isBefore(seasonEnd) ? yesterday : seasonEnd;
        if (to.isBefore(from)) {
            return Optional.of(List.of());
        }
        return temperatureProvider.fetchHourlyTemperatures(latitude, longitude, from, to);
    }

    private static Map<String, Object> chillDetails(ChillSeasonEvaluation current, ChillSeasonEvaluation previous) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("model", CHILL_MODEL);
        details.put("weatherSource", WEATHER_SOURCE);
        details.put("thresholdTarget", current.thresholdPortions());
        details.put("completionPercentage",
                ErezDynamicModelCalculator.computeCompletionPercentage(current.accumulatedPortions(), current.thresholdPortions()));
        details.put("seasonYear", current.seasonYear());
        details.put("seasonStart", current.seasonStart().toString());
        details.put("seasonEnd", current.seasonEnd().toString());
        details.put("evaluatedThrough", toText(current.evaluatedThrough()));
        details.put("completionDate", toText(current.completionDate()));
        details.put("idleDays", current.idleDays());
        details.put("seasonState", current.seasonState().name());
        details.put("daysAbove24Celsius", current.daysAbove24Celsius());
        details.put("currentWarmStreakDays", current.currentWarmStreakDays());
        details.put("longestWarmStreakDays", current.longestWarmStreakDays());
        details.put("thermalAnomaly", current.thermalAnomaly().name());
        details.put("projectionStatus", current.projectionStatus().name());
        details.put("projectedCompletionDate", toText(current.projectedCompletionDate()));
        details.put("previousSeason", previous == null ? null : previousSeason(previous));
        details.put("dailyCurve", dailyCurve(current, previous));
        return details;
    }

    private static Map<String, Object> previousSeason(ChillSeasonEvaluation previous) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("seasonYear", previous.seasonYear());
        summary.put("accumulatedPortions", previous.accumulatedPortions());
        summary.put("completionDate", toText(previous.completionDate()));
        summary.put("daysAbove24Celsius", previous.daysAbove24Celsius());
        return summary;
    }

    /**
     * One item per day of the season (June 1 - August 31). {@code portions} is null for the days not evaluated
     * yet, and {@code previousSeasonPortions} for the days the previous winter has no data, so a chart can draw the
     * whole previous winter next to the part of the current one that already happened.
     */
    private static List<Map<String, Object>> dailyCurve(ChillSeasonEvaluation current, ChillSeasonEvaluation previous) {
        var curve = new ArrayList<Map<String, Object>>();
        for (var date = current.seasonStart(); !date.isAfter(current.seasonEnd()); date = date.plusDays(1)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.toString());
            item.put("portions", current.portionsOn(date));
            item.put("previousSeasonPortions", previous == null ? null : previous.portionsOn(date.minusYears(1)));
            curve.add(item);
        }
        return curve;
    }

    private static String toText(LocalDate date) {
        return date == null ? null : date.toString();
    }
}

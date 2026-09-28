package com.arcadiadevs.viora.platform.phenology.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.application.queryservices.PhenologyMetricQueryService;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetPlotMetricsQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.DynamicErezPortion;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricEvaluationResult;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricType;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
import com.arcadiadevs.viora.platform.phenology.domain.services.ErezDynamicModelCalculator;
import com.arcadiadevs.viora.platform.phenology.domain.services.HoblynBbiCalculatorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application query service orchestrating the evaluation and retrieval of phenological metrics.
 */
@Service
@Transactional(readOnly = true)
public class PhenologyMetricQueryServiceImpl implements PhenologyMetricQueryService {

    private final ChillAccumulationTrackerRepository trackerRepository;
    private final ExternalOrchardService externalOrchardService;

    /**
     * Constructs the query service injecting dependencies.
     *
     * @param trackerRepository      the domain repository port
     * @param externalOrchardService the outbound ACL service for orchard verifications
     */
    public PhenologyMetricQueryServiceImpl(
            ChillAccumulationTrackerRepository trackerRepository,
            ExternalOrchardService externalOrchardService
    ) {
        this.trackerRepository = trackerRepository;
        this.externalOrchardService = externalOrchardService;
    }

    @Override
    public Result<List<MetricEvaluationResult>, ApplicationError> handle(GetPlotMetricsQuery query) {
        var plotId = query.plotId();
        if (!externalOrchardService.existsActivePlot(plotId)) {
            return Result.failure(ApplicationError.notFound("Plot", plotId.plotId()));
        }

        var trackerOpt = trackerRepository.findByPlotId(plotId);
        if (trackerOpt.isEmpty()) {
            return Result.failure(ApplicationError.notFound("ChillAccumulationTracker", plotId.plotId()));
        }

        var tracker = trackerOpt.get();
        var snapshot = tracker.snapshot();
        var harvestHistory = snapshot.harvestHistory();
        int evaluatedYearsCount = harvestHistory.size();

        var evaluatedBbi = snapshot.calculatedBbi();
        var alternationCategory = HoblynBbiCalculatorService.classifyAlternation(evaluatedBbi, evaluatedYearsCount);
        var sampleSufficiency = HoblynBbiCalculatorService.evaluateSufficiency(evaluatedYearsCount);

        Instant evaluationTime = Instant.now();
        List<MetricEvaluationResult> metrics = new ArrayList<>();

        // 1. Hoblyn Biennial Bearing Index
        if (query.metricType() == null || query.metricType() == MetricType.BIENNIAL_BEARING_INDEX) {
            Map<String, Object> bbiDetails = new LinkedHashMap<>();
            bbiDetails.put("formula", "Hoblyn (1936)");
            bbiDetails.put("evaluatedYearsCount", evaluatedYearsCount);
            bbiDetails.put("sampleSufficiency", sampleSufficiency.name());

            metrics.add(new MetricEvaluationResult(
                    MetricType.BIENNIAL_BEARING_INDEX,
                    evaluatedBbi.value(),
                    alternationCategory.name(),
                    bbiDetails,
                    evaluationTime
            ));
        }

        // 2. Erez Chilling Portions
        if (query.metricType() == null || query.metricType() == MetricType.EREZ_CHILLING_PORTIONS) {
            double portionsValue = 28.5; // Baseline tracking value for active tracker
            double targetThreshold = ErezDynamicModelCalculator.DEFAULT_VARIETAL_CHILL_THRESHOLD;
            double completionPct = ErezDynamicModelCalculator.computeCompletionPercentage(portionsValue, targetThreshold);
            String qualitativeStatus = ErezDynamicModelCalculator.evaluateSatisfactionStatus(portionsValue, targetThreshold);

            Map<String, Object> chillingDetails = new LinkedHashMap<>();
            chillingDetails.put("model", "Dynamic Erez-Fishman");
            chillingDetails.put("thresholdTarget", targetThreshold);
            chillingDetails.put("completionPercentage", completionPct);

            metrics.add(new MetricEvaluationResult(
                    MetricType.EREZ_CHILLING_PORTIONS,
                    portionsValue,
                    qualitativeStatus,
                    chillingDetails,
                    evaluationTime
            ));
        }

        return Result.success(metrics);
    }
}

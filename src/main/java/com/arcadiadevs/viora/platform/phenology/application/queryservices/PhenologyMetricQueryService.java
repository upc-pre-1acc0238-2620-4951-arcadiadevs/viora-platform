package com.arcadiadevs.viora.platform.phenology.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetPlotMetricsQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricEvaluationResult;

import java.util.List;

/**
 * Application query service port handling retrieval and calculation of phenological metrics.
 */
public interface PhenologyMetricQueryService {

    /**
     * Handles the evaluation and retrieval of biological and historical bearing metrics for an orchard plot.
     *
     * @param query the query containing target plotId and optional metric filter
     * @return {@link Result} containing a list of {@link MetricEvaluationResult} on success, or {@link ApplicationError} on failure
     */
    Result<List<MetricEvaluationResult>, ApplicationError> handle(GetPlotMetricsQuery query);
}

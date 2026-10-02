package com.arcadiadevs.viora.platform.thinning.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetSamplingSummaryByPlotIdQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingDetailedResult;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingStatisticalSummary;

/**
 * Application query service for retrieving plot sampling coverage.
 */
public interface GetSamplingSummaryQueryService {

    /**
     * Retrieves the statistical sampling summary for a plot and campaign.
     *
     * @param query query containing plot and campaign
     * @return successful summary or application error
     */
    Result<SamplingStatisticalSummary, ApplicationError> handle(GetSamplingSummaryByPlotIdQuery query);

    /**
     * Retrieves the summary together with ordered per-tree sampling observations.
     *
     * @param query query containing plot and campaign
     * @return detailed sampling result or application error
     */
    Result<SamplingDetailedResult, ApplicationError> handleDetailed(GetSamplingSummaryByPlotIdQuery query);
}

package com.arcadiadevs.viora.platform.thinning.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetPlotSamplingStatesQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotSamplingState;

import java.util.List;

/**
 * Application query service port for retrieving sampling states across all plots of a producer.
 */
public interface GetPlotSamplingStatesQueryService {

    /**
     * Handles the retrieval of plot sampling states for a producer and campaign year.
     *
     * @param query query criteria
     * @return list of plot sampling states or application error
     */
    Result<List<PlotSamplingState>, ApplicationError> handle(GetPlotSamplingStatesQuery query);
}

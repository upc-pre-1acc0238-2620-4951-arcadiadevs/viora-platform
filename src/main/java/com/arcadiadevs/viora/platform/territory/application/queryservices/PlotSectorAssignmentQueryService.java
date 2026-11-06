package com.arcadiadevs.viora.platform.territory.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetPlotSectorAssignmentByPlotIdAndUserIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetPlotSectorAssignmentsByUserIdQuery;

import java.util.List;

/**
 * Query service port of the sectors of member plots.
 */
public interface PlotSectorAssignmentQueryService {

    /**
     * Lists the sectors of the plots the user reaches: every member plot for the manager, the member's own plots for
     * a producer. Plots «Por confirmar» come first.
     *
     * @param query the signed-in user and an optional state
     * @return the sectors, or a 403 when the user reaches no license
     */
    Result<List<PlotSectorAssignmentView>, ApplicationError> handle(GetPlotSectorAssignmentsByUserIdQuery query);

    /**
     * Reads the sector of one plot the user reaches.
     *
     * @param query the plot and the signed-in user
     * @return the sector, a 404 when the plot has none or the user does not reach it, or a 403
     */
    Result<PlotSectorAssignmentView, ApplicationError> handle(GetPlotSectorAssignmentByPlotIdAndUserIdQuery query);
}

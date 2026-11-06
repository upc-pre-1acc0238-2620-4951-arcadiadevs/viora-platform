package com.arcadiadevs.viora.platform.territory.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetMemberPlotByIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetMemberPlotsByManagerIdQuery;

import java.util.List;

/**
 * Query service port of the member plots a manager reads, with the territorial traffic light.
 */
public interface MemberPlotQueryService {

    /**
     * Lists the plots of the members who share, in visit order.
     *
     * @param query the manager and their optional location
     * @return the plots, or a 403 when the user is not a manager with a license in force
     */
    Result<List<MemberPlotView>, ApplicationError> handle(GetMemberPlotsByManagerIdQuery query);

    /**
     * Reads one plot of a member who shares.
     *
     * @param query the plot and the manager
     * @return the plot, a 404 when it is not one of them, or a 403
     */
    Result<MemberPlotDetailView, ApplicationError> handle(GetMemberPlotByIdAndManagerIdQuery query);
}

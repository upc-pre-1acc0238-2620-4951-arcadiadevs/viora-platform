package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;

/**
 * Domain query for one plot of a member who shares with the manager's license.
 *
 * @param plotId    the requested plot
 * @param managerId the signed-in manager
 */
public record GetMemberPlotByIdAndManagerIdQuery(PlotId plotId, ManagerId managerId) {

    /**
     * Compact constructor validating that neither field is null.
     *
     * @param plotId    the requested plot
     * @param managerId the signed-in manager
     */
    public GetMemberPlotByIdAndManagerIdQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("territory.plot.id.null_or_empty");
        }
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor from raw UUID strings.
     *
     * @param rawPlotId    the raw UUID string of the plot
     * @param rawManagerId the raw UUID string of the manager
     */
    public GetMemberPlotByIdAndManagerIdQuery(String rawPlotId, String rawManagerId) {
        this(new PlotId(rawPlotId), new ManagerId(rawManagerId));
    }
}

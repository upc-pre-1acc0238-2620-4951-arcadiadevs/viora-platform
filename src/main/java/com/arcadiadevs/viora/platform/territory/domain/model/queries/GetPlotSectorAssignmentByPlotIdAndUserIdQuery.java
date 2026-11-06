package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.UserId;

/**
 * Domain query for the sector of one plot, only if the user reaches it.
 *
 * @param plotId the plot
 * @param userId the authenticated user
 */
public record GetPlotSectorAssignmentByPlotIdAndUserIdQuery(PlotId plotId, UserId userId) {

    /**
     * Compact constructor validating that neither field is null.
     *
     * @param plotId the plot
     * @param userId the authenticated user
     */
    public GetPlotSectorAssignmentByPlotIdAndUserIdQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("territory.plot.id.null_or_empty");
        }
        if (userId == null) {
            throw new IllegalArgumentException("user.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor from raw UUID strings.
     *
     * @param rawPlotId the raw UUID string of the plot
     * @param rawUserId the raw UUID string of the user
     */
    public GetPlotSectorAssignmentByPlotIdAndUserIdQuery(String rawPlotId, String rawUserId) {
        this(new PlotId(rawPlotId), new UserId(rawUserId));
    }
}

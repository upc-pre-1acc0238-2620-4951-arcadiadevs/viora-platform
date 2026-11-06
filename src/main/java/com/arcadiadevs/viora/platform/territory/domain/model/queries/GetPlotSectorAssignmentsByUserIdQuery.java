package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorStatus;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.UserId;

/**
 * Domain query for the sectors of plots a user reaches: every member plot of the manager's license, or the member's
 * own plots.
 *
 * @param userId the authenticated user
 * @param status only plots in this state; null for all
 */
public record GetPlotSectorAssignmentsByUserIdQuery(UserId userId, PlotSectorStatus status) {

    /**
     * Compact constructor validating that userId is not null.
     *
     * @param userId the authenticated user
     * @param status the optional state filter
     */
    public GetPlotSectorAssignmentsByUserIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("user.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor accepting a raw UUID string.
     *
     * @param rawUserId the raw UUID string of the user
     * @param status    the optional state filter
     */
    public GetPlotSectorAssignmentsByUserIdQuery(String rawUserId, PlotSectorStatus status) {
        this(new UserId(rawUserId), status);
    }
}

package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;

/**
 * Domain query for the alerts of the sectors of the manager's license: open, and normalized in the last days.
 *
 * @param managerId the signed-in manager
 */
public record GetSectorAlertsByManagerIdQuery(ManagerId managerId) {

    /**
     * Compact constructor validating that managerId is not null.
     *
     * @param managerId the signed-in manager
     */
    public GetSectorAlertsByManagerIdQuery {
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor accepting a raw UUID string.
     *
     * @param rawManagerId the raw UUID string of the manager
     */
    public GetSectorAlertsByManagerIdQuery(String rawManagerId) {
        this(new ManagerId(rawManagerId));
    }
}

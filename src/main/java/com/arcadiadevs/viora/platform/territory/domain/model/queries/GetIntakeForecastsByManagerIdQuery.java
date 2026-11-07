package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;

/**
 * Domain query for the campaigns whose intake the manager can see: the one in progress and the past ones with settled harvests.
 *
 * @param managerId the signed-in manager
 */
public record GetIntakeForecastsByManagerIdQuery(ManagerId managerId) {

    /**
     * Compact constructor validating that managerId is not null.
     *
     * @param managerId the signed-in manager
     */
    public GetIntakeForecastsByManagerIdQuery {
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor accepting a raw UUID string.
     *
     * @param rawManagerId the raw UUID string of the manager
     */
    public GetIntakeForecastsByManagerIdQuery(String rawManagerId) {
        this(new ManagerId(rawManagerId));
    }
}

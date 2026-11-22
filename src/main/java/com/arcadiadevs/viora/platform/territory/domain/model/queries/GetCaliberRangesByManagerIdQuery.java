package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;

/**
 * Query asking for the caliber ranges of the license the manager is in charge of.
 *
 * @param managerId the signed-in manager
 */
public record GetCaliberRangesByManagerIdQuery(ManagerId managerId) {

    /**
     * Compact constructor validating that managerId is not null.
     */
    public GetCaliberRangesByManagerIdQuery {
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor accepting a raw UUID string.
     *
     * @param rawManagerId the raw UUID string of the manager
     */
    public GetCaliberRangesByManagerIdQuery(String rawManagerId) {
        this(new ManagerId(rawManagerId));
    }
}

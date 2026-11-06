package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;

/**
 * Domain query for the members of the manager's license, sharing or not.
 *
 * @param managerId the signed-in manager
 */
public record GetCooperativeMembersByManagerIdQuery(ManagerId managerId) {

    /**
     * Compact constructor validating that managerId is not null.
     *
     * @param managerId the signed-in manager
     */
    public GetCooperativeMembersByManagerIdQuery {
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor accepting a raw UUID string.
     *
     * @param rawManagerId the raw UUID string of the manager
     */
    public GetCooperativeMembersByManagerIdQuery(String rawManagerId) {
        this(new ManagerId(rawManagerId));
    }
}

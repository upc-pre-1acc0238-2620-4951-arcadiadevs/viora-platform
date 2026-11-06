package com.arcadiadevs.viora.platform.subscription.domain.model.queries;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;

/**
 * Domain query record for retrieving the cooperative licenses a technical manager is in charge of.
 *
 * @param managerId the technical manager identifier value object
 */
public record GetAllCooperativeLicensesByManagerIdQuery(ManagerId managerId) {

    /**
     * Compact constructor validating that managerId is not null.
     *
     * @param managerId the technical manager identifier
     */
    public GetAllCooperativeLicensesByManagerIdQuery {
        if (managerId == null) {
            throw new IllegalArgumentException("subscription.manager.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor accepting a raw UUID string.
     *
     * @param rawManagerId the raw UUID string of the technical manager
     */
    public GetAllCooperativeLicensesByManagerIdQuery(String rawManagerId) {
        this(new ManagerId(rawManagerId));
    }
}

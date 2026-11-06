package com.arcadiadevs.viora.platform.subscription.domain.model.queries;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;

/**
 * Domain query record for retrieving a cooperative license only if a technical manager is in charge of it.
 *
 * @param licenseId the license identifier value object
 * @param managerId the technical manager identifier value object
 */
public record GetCooperativeLicenseByIdAndManagerIdQuery(CooperativeLicenseId licenseId, ManagerId managerId) {

    /**
     * Compact constructor validating that neither licenseId nor managerId is null.
     *
     * @param licenseId the license identifier
     * @param managerId the technical manager identifier
     */
    public GetCooperativeLicenseByIdAndManagerIdQuery {
        if (licenseId == null) {
            throw new IllegalArgumentException("subscription.license.id.null_or_empty");
        }
        if (managerId == null) {
            throw new IllegalArgumentException("subscription.manager.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor from raw UUID strings.
     *
     * @param rawLicenseId the raw UUID string of the license
     * @param rawManagerId the raw UUID string of the technical manager
     */
    public GetCooperativeLicenseByIdAndManagerIdQuery(String rawLicenseId, String rawManagerId) {
        this(new CooperativeLicenseId(rawLicenseId), new ManagerId(rawManagerId));
    }
}

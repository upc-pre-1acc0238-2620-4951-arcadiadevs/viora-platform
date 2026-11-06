package com.arcadiadevs.viora.platform.subscription.domain.model.queries;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;

/**
 * Domain query record for retrieving what a cooperative license still has to give: its free seats and hectares.
 *
 * @param licenseId the license identifier value object
 */
public record GetCooperativeLicenseCapacityQuery(CooperativeLicenseId licenseId) {

    /**
     * Compact constructor validating that licenseId is not null.
     *
     * @param licenseId the license identifier
     */
    public GetCooperativeLicenseCapacityQuery {
        if (licenseId == null) {
            throw new IllegalArgumentException("subscription.license.id.null_or_empty");
        }
    }
}

package com.arcadiadevs.viora.platform.subscription.domain.model.queries;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;

/**
 * Domain query record for retrieving a cooperative license by its identifier, such as the one sponsoring a plan.
 *
 * @param licenseId the license identifier value object
 */
public record GetCooperativeLicenseByIdQuery(CooperativeLicenseId licenseId) {

    /**
     * Compact constructor validating that licenseId is not null.
     *
     * @param licenseId the license identifier
     */
    public GetCooperativeLicenseByIdQuery {
        if (licenseId == null) {
            throw new IllegalArgumentException("subscription.license.id.null_or_empty");
        }
    }
}

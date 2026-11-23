package com.arcadiadevs.viora.platform.territory.domain.repositories;

import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.CaliberRangeSet;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;

import java.util.Optional;

/**
 * Repository of the caliber ranges of each license.
 */
public interface CaliberRangeSetRepository {

    /**
     * Finds the ranges of a license.
     *
     * @param licenseId the license
     * @return the set, or empty when the license never set ranges
     */
    Optional<CaliberRangeSet> findByLicenseId(LicenseId licenseId);

    /**
     * Stores the ranges.
     *
     * @param set the set
     * @return the set as stored
     */
    CaliberRangeSet save(CaliberRangeSet set);
}

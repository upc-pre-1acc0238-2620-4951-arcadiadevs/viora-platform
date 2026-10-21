package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;

import java.util.Optional;

/**
 * Repository port of the cooperative licenses.
 */
public interface CooperativeLicenseRepository {

    /**
     * Finds a license.
     *
     * @param id the license identity
     * @return the license, or empty if it does not exist
     */
    Optional<CooperativeLicense> findById(CooperativeLicenseId id);

    /**
     * Finds a license and locks it until the transaction ends, so concurrent redemptions commit its hectares one
     * after the other.
     *
     * @param id the license identity
     * @return the license, or empty if it does not exist
     */
    Optional<CooperativeLicense> findByIdForRedemption(CooperativeLicenseId id);

    /**
     * Checks whether any license is stored.
     *
     * @return true if at least one license exists
     */
    boolean existsAny();

    /**
     * Stores a license.
     *
     * @param license the license to store
     * @return the stored license
     */
    CooperativeLicense save(CooperativeLicense license);
}

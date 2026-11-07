package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;

import java.time.Instant;
import java.util.List;
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
     * Finds the license a technical manager is in charge of, whether or not it is still in force.
     *
     * @param managerId the manager
     * @return the license, or empty if the manager is in charge of none
     */
    Optional<CooperativeLicense> findByManagerId(ManagerId managerId);

    /**
     * Finds the license a technical manager is in charge of and locks it until the transaction ends.
     *
     * @param managerId the manager
     * @return the license, or empty if the manager is in charge of none
     */
    Optional<CooperativeLicense> findByManagerIdForUpdate(ManagerId managerId);

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

    /**
     * Lists the licenses in force at an instant that have a technical manager in charge.
     *
     * @param at the instant
     * @return the licenses
     */
    List<CooperativeLicenseId> findManagedIdsInForce(Instant at);
}

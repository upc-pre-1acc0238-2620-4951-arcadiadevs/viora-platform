package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.CooperativeLicensePersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link CooperativeLicensePersistenceEntity}.
 */
@Repository
public interface CooperativeLicensePersistenceRepository extends JpaRepository<CooperativeLicensePersistenceEntity, UUID> {

    /**
     * Finds a license and locks its row until the transaction ends.
     *
     * @param id the license identifier
     * @return the license entity, or empty if it does not exist
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select license from CooperativeLicensePersistenceEntity license where license.id = :id")
    Optional<CooperativeLicensePersistenceEntity> findLockedById(@Param("id") UUID id);

    /**
     * Finds the license a manager is in charge of.
     *
     * @param managerUserId the manager
     * @return the license entity, or empty if the manager is in charge of none
     */
    Optional<CooperativeLicensePersistenceEntity> findByManagerUserId(UUID managerUserId);

    /**
     * Finds the license a manager is in charge of and locks its row until the transaction ends.
     *
     * @param managerUserId the manager
     * @return the license entity, or empty if the manager is in charge of none
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select license from CooperativeLicensePersistenceEntity license where license.managerUserId = :managerUserId")
    Optional<CooperativeLicensePersistenceEntity> findLockedByManagerUserId(@Param("managerUserId") UUID managerUserId);

    /**
     * Lists the licenses in force at an instant that have a manager in charge.
     *
     * @param at the instant
     * @return the license identifiers
     */
    @Query("select license.id from CooperativeLicensePersistenceEntity license "
            + "where license.managerUserId is not null and license.startsAt <= :at and license.endsAt > :at")
    List<UUID> findManagedIdsInForce(@Param("at") Instant at);
}

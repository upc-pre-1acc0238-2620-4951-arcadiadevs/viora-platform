package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.CooperativeLicensePersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}

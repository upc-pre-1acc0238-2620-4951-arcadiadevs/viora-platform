package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.DataSharingConsentPersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DataSharingConsentPersistenceEntity}.
 */
@Repository
public interface DataSharingConsentPersistenceRepository
        extends JpaRepository<DataSharingConsentPersistenceEntity, UUID> {

    /**
     * Finds a consent and locks its row until the transaction ends.
     *
     * @param id the consent identifier
     * @return the consent entity, or empty if it does not exist
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select consent from DataSharingConsentPersistenceEntity consent where consent.id = :id")
    Optional<DataSharingConsentPersistenceEntity> findLockedById(@Param("id") UUID id);

    /**
     * Lists the consents of a producer, newest first.
     *
     * @param producerId the producer
     * @return the consent entities
     */
    List<DataSharingConsentPersistenceEntity> findAllByProducerIdOrderByAcceptedAtDesc(UUID producerId);

    /**
     * Lists the consents given to a license.
     *
     * @param licenseId the license
     * @return the consent entities
     */
    List<DataSharingConsentPersistenceEntity> findAllByLicenseId(UUID licenseId);
}

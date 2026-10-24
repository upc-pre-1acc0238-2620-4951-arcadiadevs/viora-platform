package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.QuotaExtensionPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link QuotaExtensionPersistenceEntity}.
 */
@Repository
public interface QuotaExtensionPersistenceRepository extends JpaRepository<QuotaExtensionPersistenceEntity, UUID> {

    /**
     * Checks whether a producer already redeemed an extension code.
     *
     * @param extensionCodeId the code
     * @param producerId      the producer
     * @return true if such a row exists
     */
    boolean existsByExtensionCodeIdAndProducerId(UUID extensionCodeId, UUID producerId);

    /** Everything of a deleted account, for the account purge. */
    List<QuotaExtensionPersistenceEntity> findAllByProducerId(UUID producerId);
}

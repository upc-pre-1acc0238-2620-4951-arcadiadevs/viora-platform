package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.QuotaExtensionCodePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link QuotaExtensionCodePersistenceEntity}.
 */
@Repository
public interface QuotaExtensionCodePersistenceRepository extends JpaRepository<QuotaExtensionCodePersistenceEntity, UUID> {

    /**
     * Finds a code by its fingerprint.
     *
     * @param codeHash the SHA-256 fingerprint of the code
     * @return the code entity, or empty if no code has this fingerprint
     */
    Optional<QuotaExtensionCodePersistenceEntity> findByCodeHash(String codeHash);
}

package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.CaliberRangeSetPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository of the caliber ranges of each license.
 */
public interface CaliberRangeSetPersistenceRepository extends JpaRepository<CaliberRangeSetPersistenceEntity, UUID> {

    Optional<CaliberRangeSetPersistenceEntity> findByLicenseId(UUID licenseId);
}

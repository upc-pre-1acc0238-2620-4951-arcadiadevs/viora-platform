package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.SectorZonePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SectorZonePersistenceEntity}.
 */
@Repository
public interface SectorZonePersistenceRepository extends JpaRepository<SectorZonePersistenceEntity, UUID> {

    /**
     * Finds a zone of a license.
     *
     * @param id        the zone
     * @param licenseId the license
     * @return the zone entity, or empty
     */
    Optional<SectorZonePersistenceEntity> findByIdAndLicenseId(UUID id, UUID licenseId);

    /**
     * Finds the zone of a license with a normalized name.
     *
     * @param licenseId      the license
     * @param normalizedName the normalized name
     * @return the zone entity, or empty
     */
    Optional<SectorZonePersistenceEntity> findByLicenseIdAndNormalizedName(UUID licenseId, String normalizedName);

    /**
     * Lists the zones of a license.
     *
     * @param licenseId the license
     * @return the zone entities
     */
    List<SectorZonePersistenceEntity> findAllByLicenseId(UUID licenseId);
}

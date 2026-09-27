package com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.entities.ChillAccumulationTrackerPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ChillAccumulationTrackerPersistenceEntity}.
 */
@Repository
public interface ChillAccumulationTrackerPersistenceRepository extends JpaRepository<ChillAccumulationTrackerPersistenceEntity, UUID> {

    /**
     * Finds tracker by plot UUID.
     *
     * @param plotId the plot UUID
     * @return an {@link Optional} containing the tracker entity
     */
    Optional<ChillAccumulationTrackerPersistenceEntity> findByPlotId(UUID plotId);
}

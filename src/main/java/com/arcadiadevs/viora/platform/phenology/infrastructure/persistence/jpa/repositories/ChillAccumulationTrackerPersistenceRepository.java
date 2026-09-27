package com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.entities.ChillAccumulationTrackerPersistenceEntity;
import org.springframework.data.jpa.repository.EntityGraph;
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
     * Finds tracker by plot UUID eagerly fetching harvest records.
     *
     * @param plotId the plot UUID
     * @return an {@link Optional} containing the tracker entity
     */
    @EntityGraph(attributePaths = {"harvestRecords"})
    Optional<ChillAccumulationTrackerPersistenceEntity> findByPlotId(UUID plotId);

    /**
     * Finds tracker by its UUID eagerly fetching harvest records.
     *
     * @param id the tracker UUID
     * @return an {@link Optional} containing the tracker entity
     */
    @Override
    @EntityGraph(attributePaths = {"harvestRecords"})
    Optional<ChillAccumulationTrackerPersistenceEntity> findById(UUID id);
}

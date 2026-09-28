package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities.TelemetrySeriesPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TelemetrySeriesPersistenceEntity}.
 */
@Repository
public interface TelemetrySeriesPersistenceRepository extends JpaRepository<TelemetrySeriesPersistenceEntity, UUID> {

    /**
     * Finds a telemetry series entity by its referenced plot unique identifier.
     *
     * @param plotId the plot UUID
     * @return an optional containing the entity if found
     */
    Optional<TelemetrySeriesPersistenceEntity> findByPlotId(UUID plotId);

    /**
     * Checks if a telemetry series exists for the given plot identifier.
     *
     * @param plotId the plot UUID
     * @return true if exists; false otherwise
     */
    boolean existsByPlotId(UUID plotId);
}

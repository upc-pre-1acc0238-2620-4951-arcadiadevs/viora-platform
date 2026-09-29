package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities.HourlyTelemetryReadingPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link HourlyTelemetryReadingPersistenceEntity}.
 */
@Repository
public interface HourlyTelemetryReadingPersistenceRepository extends JpaRepository<HourlyTelemetryReadingPersistenceEntity, UUID> {

    /**
     * Finds all readings for a plot in chronological order.
     */
    List<HourlyTelemetryReadingPersistenceEntity> findAllByPlotIdOrderByReadingTimestampAsc(UUID plotId);

    /**
     * Finds readings for a plot between a start and end instant in chronological order.
     */
    List<HourlyTelemetryReadingPersistenceEntity> findAllByPlotIdAndReadingTimestampBetweenOrderByReadingTimestampAsc(
            UUID plotId,
            Instant startDate,
            Instant endDate
    );

    /**
     * Finds readings for a plot after or at a start instant in chronological order.
     */
    List<HourlyTelemetryReadingPersistenceEntity> findAllByPlotIdAndReadingTimestampGreaterThanEqualOrderByReadingTimestampAsc(
            UUID plotId,
            Instant startDate
    );

    /**
     * Finds readings for a plot before or at an end instant in chronological order.
     */
    List<HourlyTelemetryReadingPersistenceEntity> findAllByPlotIdAndReadingTimestampLessThanEqualOrderByReadingTimestampAsc(
            UUID plotId,
            Instant endDate
    );
}

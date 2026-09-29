package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.TelemetrySeries;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.TelemetrySeriesSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities.HourlyTelemetryReadingPersistenceEntity;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities.TelemetrySeriesPersistenceEntity;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Unified persistence assembler bridging domain {@link TelemetrySeries} aggregate roots
 * and JPA {@link TelemetrySeriesPersistenceEntity} representations.
 */
public final class TelemetrySeriesPersistenceAssembler {

    private TelemetrySeriesPersistenceAssembler() {
    }

    /**
     * Converts a domain aggregate to a new JPA persistence entity.
     *
     * @param domain the domain aggregate
     * @return the mapped JPA entity
     */
    public static TelemetrySeriesPersistenceEntity toPersistenceFromDomain(TelemetrySeries domain) {
        if (domain == null) {
            throw new IllegalArgumentException("telemetry.series.snapshot.null");
        }
        var snap = domain.snapshot();
        var entity = new TelemetrySeriesPersistenceEntity();
        entity.setId(UUID.fromString(snap.id().seriesId()));
        entity.setPlotId(UUID.fromString(snap.plotId().plotId()));
        if (snap.sensorNodeId() != null) {
            entity.setSensorNodeId(UUID.fromString(snap.sensorNodeId().deviceId()));
        }
        entity.setCurrentStatus(snap.currentStatus().name());

        var childEntities = new ArrayList<HourlyTelemetryReadingPersistenceEntity>();
        for (var readingSnap : snap.readings()) {
            var child = new HourlyTelemetryReadingPersistenceEntity();
            child.setId(UUID.fromString(readingSnap.id().readingId()));
            child.setSeries(entity);
            child.setPlotId(UUID.fromString(snap.plotId().plotId()));
            child.setReadingTimestamp(readingSnap.timestamp().timestamp());
            child.setTemperature(readingSnap.temperature().celsius());
            child.setRelativeHumidity(readingSnap.relativeHumidity().percentage());
            child.setSoilMoisture(readingSnap.soilMoisture().percentage());
            child.setSolarRadiation(readingSnap.solarRadiation().wattsPerSquareMeter());
            if (readingSnap.stemWaterPotential() != null) {
                child.setStemWaterPotential(readingSnap.stemWaterPotential().stemWaterPotentialMpa());
            }
            childEntities.add(child);
        }
        entity.setReadings(childEntities);
        // Revision is intentionally left null for transient entities so Hibernate executes an INSERT
        return entity;
    }

    /**
     * Synchronizes state from the domain aggregate into an existing managed JPA entity.
     *
     * @param target the managed JPA entity
     * @param domain the domain aggregate with updated state
     * @return the updated target entity
     */
    public static TelemetrySeriesPersistenceEntity updateEntityFromDomain(
            TelemetrySeriesPersistenceEntity target,
            TelemetrySeries domain
    ) {
        if (target == null) {
            throw new IllegalArgumentException("target.entity.null");
        }
        if (domain == null) {
            throw new IllegalArgumentException("telemetry.series.snapshot.null");
        }
        var snap = domain.snapshot();
        target.setCurrentStatus(snap.currentStatus().name());
        if (snap.sensorNodeId() != null) {
            target.setSensorNodeId(UUID.fromString(snap.sensorNodeId().deviceId()));
        }

        // Add newly ingested readings that aren't already present in target
        var existingReadingIds = target.getReadings().stream()
                .map(HourlyTelemetryReadingPersistenceEntity::getId)
                .toList();

        for (var readingSnap : snap.readings()) {
            var readingUuid = UUID.fromString(readingSnap.id().readingId());
            if (!existingReadingIds.contains(readingUuid)) {
                var child = new HourlyTelemetryReadingPersistenceEntity();
                child.setId(readingUuid);
                child.setSeries(target);
                child.setPlotId(UUID.fromString(snap.plotId().plotId()));
                child.setReadingTimestamp(readingSnap.timestamp().timestamp());
                child.setTemperature(readingSnap.temperature().celsius());
                child.setRelativeHumidity(readingSnap.relativeHumidity().percentage());
                child.setSoilMoisture(readingSnap.soilMoisture().percentage());
                child.setSolarRadiation(readingSnap.solarRadiation().wattsPerSquareMeter());
                if (readingSnap.stemWaterPotential() != null) {
                    child.setStemWaterPotential(readingSnap.stemWaterPotential().stemWaterPotentialMpa());
                }
                target.getReadings().add(child);
            }
        }
        return target;
    }

    /**
     * Reconstitutes a domain aggregate from a JPA persistence entity.
     *
     * @param entity the managed JPA entity
     * @return the reconstituted {@link TelemetrySeries} aggregate root
     */
    public static TelemetrySeries toDomainFromPersistence(TelemetrySeriesPersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity.null");
        }
        var readingSnapshots = entity.getReadings().stream()
                .map(TelemetrySeriesPersistenceAssembler::toReadingSnapshot)
                .toList();

        var snapshot = new TelemetrySeriesSnapshot(
                new TelemetrySeriesId(entity.getId().toString()),
                new PlotId(entity.getPlotId().toString()),
                entity.getSensorNodeId() != null ? new DeviceId(entity.getSensorNodeId().toString()) : null,
                TelemetrySeriesStatus.valueOf(entity.getCurrentStatus()),
                readingSnapshots,
                entity.getRevision()
        );
        return TelemetrySeries.reconstitute(snapshot);
    }

    /**
     * Maps an individual reading JPA entity to an immutable domain snapshot.
     *
     * @param entity the reading entity
     * @return the reading snapshot
     */
    public static HourlyTelemetryReadingSnapshot toReadingSnapshot(HourlyTelemetryReadingPersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity.null");
        }
        return new HourlyTelemetryReadingSnapshot(
                new HourlyReadingId(entity.getId().toString()),
                new ReadingTimestamp(entity.getReadingTimestamp()),
                new AmbientTemperature(entity.getTemperature()),
                new RelativeHumidity(entity.getRelativeHumidity()),
                new SoilMoisture(entity.getSoilMoisture()),
                new SolarRadiation(entity.getSolarRadiation()),
                entity.getStemWaterPotential() != null ? new StemWaterPotential(entity.getStemWaterPotential()) : null
        );
    }
}

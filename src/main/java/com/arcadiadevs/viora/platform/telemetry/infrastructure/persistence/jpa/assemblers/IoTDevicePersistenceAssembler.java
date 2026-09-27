package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDeviceSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities.IoTDevicePersistenceEntity;

import java.util.UUID;

/**
 * Unified persistence assembler translating between the domain {@link IoTDevice} aggregate
 * and the JPA {@link IoTDevicePersistenceEntity}.
 */
public final class IoTDevicePersistenceAssembler {

    private IoTDevicePersistenceAssembler() {
    }

    /**
     * Converts a domain aggregate into a new JPA persistence entity.
     *
     * @param domain the domain aggregate to map
     * @return the corresponding JPA persistence entity
     */
    public static IoTDevicePersistenceEntity toPersistenceFromDomain(IoTDevice domain) {
        return toEntity(domain);
    }

    /**
     * Maps a domain {@link IoTDevice} aggregate into a JPA {@link IoTDevicePersistenceEntity}.
     *
     * @param domain the domain aggregate to map
     * @return the corresponding JPA persistence entity
     */
    public static IoTDevicePersistenceEntity toEntity(IoTDevice domain) {
        if (domain == null) {
            throw new IllegalArgumentException("device.domain.null");
        }
        var snap = domain.snapshot();

        var entity = new IoTDevicePersistenceEntity();
        entity.setId(UUID.fromString(snap.id().deviceId()));
        entity.setPlotId(snap.plotId());
        entity.setName(snap.name());
        entity.setType(snap.type());
        entity.setDepth(snap.depth());
        entity.setSoilTextureType(snap.soilTextureType());
        entity.setCalibrationMultiplier(snap.calibrationMultiplier());
        entity.setStatus(snap.status());
        entity.setLastReadingTimestamp(snap.lastReadingTimestamp());
        if (snap.revision() != null && snap.revision() > 0L) {
            entity.setRevision(snap.revision());
        }

        return entity;
    }

    /**
     * Updates an existing managed JPA entity state from the domain aggregate snapshot,
     * allowing JPA / Hibernate to handle optimistic locking revisions naturally.
     *
     * @param target the managed JPA entity to update
     * @param domain the domain aggregate containing updated state
     * @return the updated JPA entity
     */
    public static IoTDevicePersistenceEntity updateEntityFromDomain(IoTDevicePersistenceEntity target, IoTDevice domain) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("device.entity_or_domain.null");
        }
        var snap = domain.snapshot();
        target.setName(snap.name());
        target.setDepth(snap.depth());
        target.setSoilTextureType(snap.soilTextureType());
        target.setCalibrationMultiplier(snap.calibrationMultiplier());
        target.setStatus(snap.status());
        target.setLastReadingTimestamp(snap.lastReadingTimestamp());
        return target;
    }

    /**
     * Maps a JPA {@link IoTDevicePersistenceEntity} into a domain {@link IoTDevice} aggregate.
     *
     * @param entity the JPA entity to map
     * @return the reconstituted domain IoTDevice aggregate
     */
    public static IoTDevice toDomainFromPersistence(IoTDevicePersistenceEntity entity) {
        return toDomain(entity);
    }

    /**
     * Maps a JPA {@link IoTDevicePersistenceEntity} into a domain {@link IoTDevice} aggregate.
     *
     * @param entity the JPA entity to map
     * @return the reconstituted domain IoTDevice aggregate
     */
    public static IoTDevice toDomain(IoTDevicePersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("device.entity.null");
        }

        var snapshot = new IoTDeviceSnapshot(
                new DeviceId(entity.getId().toString()),
                entity.getPlotId(),
                entity.getName(),
                entity.getType(),
                entity.getDepth() != null ? entity.getDepth() : SensorDepth.none(),
                entity.getSoilTextureType() != null ? entity.getSoilTextureType() : SoilTextureType.NOT_APPLICABLE,
                entity.getCalibrationMultiplier() != null ? entity.getCalibrationMultiplier() : CalibrationMultiplier.defaultMultiplier(),
                entity.getStatus(),
                entity.getLastReadingTimestamp(),
                entity.getRevision()
        );

        return IoTDevice.reconstitute(snapshot);
    }
}

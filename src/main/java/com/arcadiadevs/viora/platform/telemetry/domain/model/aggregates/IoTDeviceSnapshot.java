package com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;

import java.time.Instant;

/**
 * Immutable snapshot representing the integral state of an {@link IoTDevice} aggregate.
 * Used by infrastructure assemblers for persistence and presentation without breaking encapsulation.
 *
 * @param id                    the unique device identifier
 * @param plotId                the logical reference to the instrumented plot
 * @param name                  the descriptive device name
 * @param type                  the functional classification (MICROCLIMATE or SOIL_PROBE)
 * @param depth                 the installation depth
 * @param soilTextureType       the soil textural classification
 * @param calibrationMultiplier the empirical adjustment multiplier
 * @param status                the operational status
 * @param lastReadingTimestamp  the timestamp of the most recent telemetry reading
 * @param revision              the optimistic locking revision number
 * @param pausedAt              when the producer paused the node; null unless it is paused
 */
public record IoTDeviceSnapshot(
        DeviceId id,
        PlotId plotId,
        DeviceName name,
        DeviceType type,
        SensorDepth depth,
        SoilTextureType soilTextureType,
        CalibrationMultiplier calibrationMultiplier,
        DeviceStatus status,
        Instant lastReadingTimestamp,
        Long revision,
        Instant pausedAt
) {

    /**
     * Snapshot of a node that is not paused.
     */
    public IoTDeviceSnapshot(DeviceId id, PlotId plotId, DeviceName name, DeviceType type, SensorDepth depth,
                             SoilTextureType soilTextureType, CalibrationMultiplier calibrationMultiplier,
                             DeviceStatus status, Instant lastReadingTimestamp, Long revision) {
        this(id, plotId, name, type, depth, soilTextureType, calibrationMultiplier, status, lastReadingTimestamp,
                revision, null);
    }

}

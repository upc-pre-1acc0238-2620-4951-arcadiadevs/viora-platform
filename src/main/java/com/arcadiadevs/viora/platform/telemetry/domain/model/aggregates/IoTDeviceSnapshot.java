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
        Long revision
) {
}

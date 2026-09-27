package com.arcadiadevs.viora.platform.telemetry.domain.model.events;

import java.time.Instant;

/**
 * Domain event dispatched when an IoT device is calibrated with new empirical multipliers and soil factors.
 *
 * @param deviceId              the unique identifier of the calibrated device
 * @param plotId                the identifier of the monitored plot
 * @param calibrationMultiplier the updated empirical calibration multiplier
 * @param soilTextureType       the updated soil texture string
 * @param depthCm               the updated sensor depth in centimeters
 * @param revision              the updated aggregate revision after calibration
 * @param occurredOn            the exact timestamp when the calibration occurred
 */
public record IoTDeviceCalibratedEvent(
        String deviceId,
        String plotId,
        Double calibrationMultiplier,
        String soilTextureType,
        Integer depthCm,
        Long revision,
        Instant occurredOn
) {

    /**
     * Compact constructor enforcing non-null event invariants.
     *
     * @throws IllegalArgumentException if mandatory fields are missing
     */
    public IoTDeviceCalibratedEvent {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("device.event.device_id.null");
        }
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("device.event.plot_id.null");
        }
        if (calibrationMultiplier == null) {
            throw new IllegalArgumentException("device.calibration.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("device.event.occurred_on.null");
        }
    }
}

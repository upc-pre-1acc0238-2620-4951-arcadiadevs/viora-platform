package com.arcadiadevs.viora.platform.telemetry.domain.model.commands;

/**
 * Domain command to calibrate the empirical offset and soil factors of an existing IoT device.
 * Follows strict Model A rules: validates non-null mandatory fields with i18n keys without business logic.
 *
 * @param plotId                the unique identifier of the plot instrumented
 * @param deviceId              the unique identifier of the device to calibrate
 * @param calibrationMultiplier the empirical adjustment multiplier
 * @param soilTextureType       the soil textural classification string (optional, nullable)
 * @param depthCm               the updated installation depth in centimeters (optional, nullable)
 * @param expectedRevision      the expected optimistic concurrency revision (optional, nullable)
 */
public record CalibrateIoTDeviceCommand(
        String plotId,
        String deviceId,
        Double calibrationMultiplier,
        String soilTextureType,
        Integer depthCm,
        Long expectedRevision
) {

    /**
     * Compact constructor enforcing non-null presence of mandatory command attributes.
     *
     * @throws IllegalArgumentException if plotId, deviceId, or calibrationMultiplier is null or invalid
     */
    public CalibrateIoTDeviceCommand {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("device.id.null_or_empty");
        }
        if (calibrationMultiplier == null) {
            throw new IllegalArgumentException("device.calibration.null");
        }
    }
}

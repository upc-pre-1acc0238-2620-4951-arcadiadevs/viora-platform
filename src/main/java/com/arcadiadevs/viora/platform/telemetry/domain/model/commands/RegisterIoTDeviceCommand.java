package com.arcadiadevs.viora.platform.telemetry.domain.model.commands;

/**
 * Domain command to register and bind a new virtual IoT device or sensor probe to an orchard plot.
 * Follows strict Model A rules: validates non-null mandatory fields with i18n keys without business logic.
 *
 * @param plotId                the unique identifier of the plot being instrumented
 * @param name                  the descriptive name of the device
 * @param type                  the functional classification of the device (MICROCLIMATE or SOIL_PROBE)
 * @param depthCm               the installation depth in centimeters (optional for microclimate)
 * @param soilTextureType       the soil textural classification string (optional for microclimate)
 * @param calibrationMultiplier the empirical adjustment factor (optional, defaults to 1.0)
 */
public record RegisterIoTDeviceCommand(
        String plotId,
        String name,
        String type,
        Integer depthCm,
        String soilTextureType,
        Double calibrationMultiplier
) {

    /**
     * Compact constructor enforcing non-null presence of mandatory command attributes.
     *
     * @throws IllegalArgumentException if plotId, name, or type is null
     */
    public RegisterIoTDeviceCommand {
        if (plotId == null) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        if (name == null) {
            throw new IllegalArgumentException("device.name.blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("device.type.blank");
        }
    }
}

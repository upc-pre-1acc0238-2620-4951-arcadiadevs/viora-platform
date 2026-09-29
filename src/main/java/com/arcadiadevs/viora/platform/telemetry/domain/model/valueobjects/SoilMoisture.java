package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Value object representing volumetric soil moisture or calibrated moisture percentage (%).
 * Enforces edaphic moisture boundaries [0.0, 100.0].
 *
 * @param percentage the volumetric soil moisture percentage
 */
public record SoilMoisture(Double percentage) {

    public static final double MIN_SOIL_MOISTURE_PERCENT = 0.0;
    public static final double MAX_SOIL_MOISTURE_PERCENT = 100.0;

    /**
     * Compact constructor validating soil moisture boundaries.
     *
     * @throws IllegalArgumentException if percentage is null or out of range
     */
    public SoilMoisture {
        if (percentage == null) {
            throw new IllegalArgumentException("telemetry.soil_moisture.null");
        }
        if (percentage < MIN_SOIL_MOISTURE_PERCENT || percentage > MAX_SOIL_MOISTURE_PERCENT) {
            throw new IllegalArgumentException("telemetry.soil_moisture.invalid_percentage");
        }
    }
}

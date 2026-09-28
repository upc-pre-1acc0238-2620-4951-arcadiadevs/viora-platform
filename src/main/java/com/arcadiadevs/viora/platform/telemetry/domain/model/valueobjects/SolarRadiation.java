package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Value object representing solar global horizontal irradiance in Watts per square meter (W/m²).
 * Enforces non-negative physical boundary.
 *
 * @param wattsPerSquareMeter the solar radiation in W/m²
 */
public record SolarRadiation(Double wattsPerSquareMeter) {

    /**
     * Compact constructor validating solar radiation.
     *
     * @throws IllegalArgumentException if wattsPerSquareMeter is null or negative
     */
    public SolarRadiation {
        if (wattsPerSquareMeter == null) {
            throw new IllegalArgumentException("telemetry.solar_radiation.null");
        }
        if (wattsPerSquareMeter < 0.0) {
            throw new IllegalArgumentException("telemetry.solar_radiation.negative");
        }
    }
}

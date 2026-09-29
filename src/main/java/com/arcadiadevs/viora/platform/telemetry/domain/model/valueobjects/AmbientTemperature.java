package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Value object representing ambient temperature in degrees Celsius (°C).
 * Enforces biological and terrestrial meteorological limits [-30.0, 70.0].
 *
 * @param celsius the temperature value in degrees Celsius
 */
public record AmbientTemperature(Double celsius) {

    public static final double MIN_TEMPERATURE_CELSIUS = -30.0;
    public static final double MAX_TEMPERATURE_CELSIUS = 70.0;

    /**
     * Compact constructor validating meteorological range limits.
     *
     * @throws IllegalArgumentException if celsius is null or out of range
     */
    public AmbientTemperature {
        if (celsius == null) {
            throw new IllegalArgumentException("telemetry.temperature.null");
        }
        if (celsius < MIN_TEMPERATURE_CELSIUS || celsius > MAX_TEMPERATURE_CELSIUS) {
            throw new IllegalArgumentException("telemetry.temperature.out_of_range");
        }
    }
}

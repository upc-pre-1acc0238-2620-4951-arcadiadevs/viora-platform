package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Value object representing wind speed in kilometers per hour (km/h).
 * Enforces terrestrial meteorological limits in the range [0.0, 300.0].
 *
 * @param kmh wind speed in km/h
 */
public record WindSpeed(Double kmh) {

    public static final double MIN_WIND_SPEED_KMH = 0.0;
    public static final double MAX_WIND_SPEED_KMH = 300.0;

    /**
     * Compact constructor enforcing positive and realistic terrestrial wind speed limits.
     *
     * @throws IllegalArgumentException if kmh is null or out of range
     */
    public WindSpeed {
        if (kmh == null) {
            throw new IllegalArgumentException("telemetry.wind_speed.null");
        }
        if (kmh < MIN_WIND_SPEED_KMH || kmh > MAX_WIND_SPEED_KMH) {
            throw new IllegalArgumentException("telemetry.wind_speed.out_of_range");
        }
    }
}

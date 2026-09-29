package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Value object representing relative air humidity in percentage (%).
 * Enforces atmospheric physics boundaries [0.0, 100.0].
 *
 * @param percentage the relative humidity percentage
 */
public record RelativeHumidity(Double percentage) {

    public static final double MIN_HUMIDITY_PERCENT = 0.0;
    public static final double MAX_HUMIDITY_PERCENT = 100.0;

    /**
     * Compact constructor validating relative humidity percentage limits.
     *
     * @throws IllegalArgumentException if percentage is null or out of range
     */
    public RelativeHumidity {
        if (percentage == null) {
            throw new IllegalArgumentException("telemetry.relative_humidity.null");
        }
        if (percentage < MIN_HUMIDITY_PERCENT || percentage > MAX_HUMIDITY_PERCENT) {
            throw new IllegalArgumentException("telemetry.relative_humidity.invalid_percentage");
        }
    }
}

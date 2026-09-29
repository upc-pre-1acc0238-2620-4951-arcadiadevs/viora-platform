package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Value object representing a percentage metric in meteorological and agroclimatic contexts.
 * Enforces bounded percentage values in the range [0.0, 100.0].
 *
 * @param value percentage value between 0.0 and 100.0
 */
public record Percentage(Double value) {

    public static final double MIN_PERCENTAGE = 0.0;
    public static final double MAX_PERCENTAGE = 100.0;

    /**
     * Compact constructor enforcing bounded percentage limits.
     *
     * @throws IllegalArgumentException if value is null or out of [0.0, 100.0] range
     */
    public Percentage {
        if (value == null) {
            throw new IllegalArgumentException("telemetry.percentage.null");
        }
        if (value < MIN_PERCENTAGE || value > MAX_PERCENTAGE) {
            throw new IllegalArgumentException("telemetry.percentage.out_of_range");
        }
    }
}

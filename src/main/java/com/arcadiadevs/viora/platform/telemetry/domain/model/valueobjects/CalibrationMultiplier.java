package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Value object representing an empirical calibration multiplier applied to raw sensor measurements.
 * Invariant: Must not be null and must fall strictly within the agronomic adjustment range [0.50, 2.00].
 *
 * @param value the multiplier factor
 */
public record CalibrationMultiplier(Double value) {

    /**
     * Standard baseline calibration factor with zero adjustment (1.0).
     */
    public static final double DEFAULT_VALUE = 1.0;

    /**
     * Minimum allowable calibration factor.
     */
    public static final double MIN_VALUE = 0.50;

    /**
     * Maximum allowable calibration factor.
     */
    public static final double MAX_VALUE = 2.00;

    /**
     * Compact constructor enforcing non-null and valid range invariants.
     *
     * @throws IllegalArgumentException if value is null or outside the range [0.50, 2.00]
     */
    public CalibrationMultiplier {
        if (value == null) {
            throw new IllegalArgumentException("device.calibration.null");
        }
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new IllegalArgumentException("device.calibration.out_of_range");
        }
    }

    /**
     * Factory method creating the default neutral calibration multiplier (1.0).
     *
     * @return a {@link CalibrationMultiplier} initialized to 1.0
     */
    public static CalibrationMultiplier defaultMultiplier() {
        return new CalibrationMultiplier(DEFAULT_VALUE);
    }
}

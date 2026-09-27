package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Value object representing the physical installation depth of an edaphic sensor probe in centimeters.
 * Required for soil probes (typically 30 cm or 60 cm), optional or null for atmospheric microclimate stations.
 *
 * @param depthCm the depth in centimeters, or null if not applicable
 */
public record SensorDepth(Integer depthCm) {

    /**
     * Compact constructor validating depth bounds when present.
     *
     * @throws IllegalArgumentException if depthCm is less than or equal to 0, or exceeds 200 cm
     */
    public SensorDepth {
        if (depthCm != null && (depthCm <= 0 || depthCm > 200)) {
            throw new IllegalArgumentException("device.depth.invalid");
        }
    }

    /**
     * Factory method creating a SensorDepth instance for a specified centimeter value.
     *
     * @param depthCm the depth in centimeters
     * @return a valid {@link SensorDepth} instance
     */
    public static SensorDepth of(Integer depthCm) {
        return new SensorDepth(depthCm);
    }

    /**
     * Factory method representing the absence of an installation depth (e.g. for microclimate nodes).
     *
     * @return a {@link SensorDepth} instance with null depth
     */
    public static SensorDepth none() {
        return new SensorDepth(null);
    }

    /**
     * Checks if this instance has an assigned depth value.
     *
     * @return true if depthCm is present, false otherwise
     */
    public boolean isPresent() {
        return depthCm != null;
    }
}

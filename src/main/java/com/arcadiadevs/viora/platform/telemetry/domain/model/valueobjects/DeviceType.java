package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Functional classification of an IoT device in the orchard.
 */
public enum DeviceType {
    MICROCLIMATE,
    SOIL_PROBE;

    /**
     * Parses and validates a device type from its string representation.
     *
     * @param value the raw device type string
     * @return the corresponding {@link DeviceType}
     * @throws IllegalArgumentException if the value is null, blank, or not recognized
     */
    public static DeviceType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("device.type.blank");
        }
        try {
            return DeviceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("device.type.unknown");
        }
    }
}

package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Operational lifecycle status of an IoT sensor node.
 */
public enum DeviceStatus {
    ACTIVE,
    PAUSED,
    UNLINKED;

    /**
     * Parses and validates a device status from its string representation.
     *
     * @param value the raw status string
     * @return the corresponding {@link DeviceStatus}
     * @throws IllegalArgumentException if the value is null, blank, or not recognized
     */
    public static DeviceStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("device.status.blank");
        }
        try {
            return DeviceStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("device.status.unknown");
        }
    }
}

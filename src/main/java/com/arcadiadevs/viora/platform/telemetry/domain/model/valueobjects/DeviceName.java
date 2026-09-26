package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Value Object representing the descriptive name of an IoT sensor device.
 * Invariant: Must not be blank and trimmed length must be between 2 and 100 characters.
 *
 * @param value the string representation of the device name
 */
public record DeviceName(String value) {

    /**
     * Compact constructor enforcing name length and non-blank invariants.
     *
     * @throws IllegalArgumentException if the name is null, blank, or has invalid length
     */
    public DeviceName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("device.name.blank");
        }
        var trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new IllegalArgumentException("device.name.invalid_length");
        }
        value = trimmed;
    }
}

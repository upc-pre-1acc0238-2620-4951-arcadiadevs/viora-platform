package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the unique identifier of an IoT device.
 * Encapsulates a valid UUID string and enforces domain identity invariants.
 *
 * @param deviceId the unique device identifier UUID string
 */
public record DeviceId(String deviceId) {

    /**
     * Default constructor generating a new random UUID.
     */
    public DeviceId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor validating that the identifier is a non-blank, valid UUID string.
     *
     * @throws IllegalArgumentException if the deviceId is null, blank, or malformed
     */
    public DeviceId {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("device.id.null_or_empty");
        }
        try {
            deviceId = UUID.fromString(deviceId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("device.id.invalid_uuid", exception);
        }
    }
}

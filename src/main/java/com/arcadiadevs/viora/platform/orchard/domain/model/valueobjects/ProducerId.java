package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the producer identifier.
 * Serves as the cross-context logical reference to the UserId in the IAM Bounded Context.
 *
 * <p>
 * This value object is used to represent the producer identifier.
 * The identifier is stored as a UUID string to avoid unnecessary coupling.
 * It throws an IllegalArgumentException if the producer id is null, empty, or not a valid UUID.
 * </p>
 *
 * @param producerId The producer id. It must be a valid UUID string.
 */
public record ProducerId(String producerId) {

    /**
     * Default constructor.
     * Generates a new random UUID and sets it as the producer id string.
     */
    public ProducerId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor for ProducerId.
     * Validates that the producerId is a valid UUID string.
     *
     * @throws IllegalArgumentException if the producerId is null, blank, or not a valid UUID.
     */
    public ProducerId {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
        try {
            producerId = UUID.fromString(producerId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("producer.id.invalid_uuid", exception);
        }
    }
}

package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.util.UUID;

/**
 * Logical reference to a producer, kept by the IAM context.
 *
 * @param producerId the identifier as a lowercase UUID string
 */
public record ProducerId(String producerId) {

    /**
     * Generates a new random identifier.
     */
    public ProducerId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
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

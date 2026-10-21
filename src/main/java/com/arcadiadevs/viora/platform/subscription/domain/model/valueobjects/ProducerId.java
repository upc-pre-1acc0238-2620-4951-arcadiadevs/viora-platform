package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object referencing the producer, the IAM user that owns the subscription.
 * <p>The identifier is stored as a UUID string. It throws an IllegalArgumentException if it is null, blank, or not a
 * valid UUID.</p>
 *
 * @param producerId the identifier; it must be a valid UUID string
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

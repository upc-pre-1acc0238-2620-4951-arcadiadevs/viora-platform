package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object referencing a cooperative, whose identity and roster belong to Territory.
 * <p>The identifier is stored as a UUID string. It throws an IllegalArgumentException if it is null, blank, or not a
 * valid UUID.</p>
 *
 * @param cooperativeId the identifier; it must be a valid UUID string
 */
public record CooperativeId(String cooperativeId) {

    /**
     * Generates a new random identifier.
     */
    public CooperativeId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public CooperativeId {
        if (cooperativeId == null || cooperativeId.isBlank()) {
            throw new IllegalArgumentException("subscription.cooperative.id.null_or_empty");
        }
        try {
            cooperativeId = UUID.fromString(cooperativeId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("subscription.cooperative.id.invalid_uuid", exception);
        }
    }
}

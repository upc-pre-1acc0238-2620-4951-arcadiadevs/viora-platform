package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object identifying a cooperative license.
 * <p>The identifier is stored as a UUID string. It throws an IllegalArgumentException if it is null, blank, or not a
 * valid UUID.</p>
 *
 * @param cooperativeLicenseId the identifier; it must be a valid UUID string
 */
public record CooperativeLicenseId(String cooperativeLicenseId) {

    /**
     * Generates a new random identifier.
     */
    public CooperativeLicenseId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public CooperativeLicenseId {
        if (cooperativeLicenseId == null || cooperativeLicenseId.isBlank()) {
            throw new IllegalArgumentException("subscription.license.id.null_or_empty");
        }
        try {
            cooperativeLicenseId = UUID.fromString(cooperativeLicenseId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("subscription.license.id.invalid_uuid", exception);
        }
    }
}

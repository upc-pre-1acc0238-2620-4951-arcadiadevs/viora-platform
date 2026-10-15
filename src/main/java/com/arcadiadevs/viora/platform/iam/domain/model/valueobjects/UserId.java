package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the immutable unique identifier of a user account.
 *
 * @param userId the UUID string representation
 */
public record UserId(String userId) {

    /**
     * Default constructor generating a new random UUID.
     */
    public UserId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor validating non-null, non-blank valid UUID.
     *
     * @throws IllegalArgumentException if the userId is null, blank, or malformed
     */
    public UserId {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user.id.null_or_empty");
        }
        try {
            userId = UUID.fromString(userId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("user.id.invalid_uuid", ex);
        }
    }
}

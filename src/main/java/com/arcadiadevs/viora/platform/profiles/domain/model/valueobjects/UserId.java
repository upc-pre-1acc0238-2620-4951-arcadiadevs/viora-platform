package com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the logical identity reference to the user account in IAM.
 *
 * @param userId the UUID string representation
 */
public record UserId(String userId) {

    /**
     * Compact constructor validating that userId is a valid UUID string.
     *
     * @throws IllegalArgumentException if userId is null, blank, or malformed
     */
    public UserId {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("profile.user_id.null_or_empty");
        }
        try {
            userId = UUID.fromString(userId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("profile.user_id.invalid_uuid", ex);
        }
    }
}

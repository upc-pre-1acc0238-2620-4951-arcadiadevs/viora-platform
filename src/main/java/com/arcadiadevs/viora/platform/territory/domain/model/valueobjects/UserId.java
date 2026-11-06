package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.util.UUID;

/**
 * Logical reference to the signed-in user, kept by the IAM context: a technical manager or a producer, depending on
 * its role.
 *
 * @param userId the identifier as a lowercase UUID string
 */
public record UserId(String userId) {

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public UserId {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user.id.null_or_empty");
        }
        try {
            userId = UUID.fromString(userId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("user.id.invalid_uuid", exception);
        }
    }
}

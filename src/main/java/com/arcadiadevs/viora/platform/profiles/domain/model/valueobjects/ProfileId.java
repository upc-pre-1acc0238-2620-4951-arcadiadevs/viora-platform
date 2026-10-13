package com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the unique identifier of a user profile.
 *
 * @param profileId the UUID string representation
 */
public record ProfileId(String profileId) {

    /**
     * Default constructor generating a new random UUID.
     */
    public ProfileId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor validating that profileId is a non-empty, valid UUID.
     *
     * @throws IllegalArgumentException if profileId is null, blank, or malformed
     */
    public ProfileId {
        if (profileId == null || profileId.isBlank()) {
            throw new IllegalArgumentException("profile.id.null_or_empty");
        }
        try {
            profileId = UUID.fromString(profileId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("profile.id.invalid_uuid", ex);
        }
    }
}

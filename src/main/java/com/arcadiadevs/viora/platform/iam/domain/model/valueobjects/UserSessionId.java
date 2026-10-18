package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the unique identifier of a user session.
 *
 * @param sessionId the UUID string representation
 */
public record UserSessionId(String sessionId) {

    /**
     * Default constructor generating a new random UUID.
     */
    public UserSessionId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor validating non-null, non-blank valid UUID.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or malformed
     */
    public UserSessionId {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("user.session.id.null_or_empty");
        }
        try {
            sessionId = UUID.fromString(sessionId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("user.session.id.invalid_uuid", ex);
        }
    }
}

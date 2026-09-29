package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value Object referencing an external User account (technician or producer).
 *
 * @param actorId the string UUID value
 */
public record UserId(String actorId) {

    /**
     * Compact constructor validating that actorId is a non-null valid UUID.
     */
    public UserId {
        if (actorId == null || actorId.trim().isEmpty()) {
            throw new IllegalArgumentException("thinning.actor.id.null_or_empty");
        }
        try {
            actorId = UUID.fromString(actorId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("thinning.actor.id.invalid_uuid", e);
        }
    }
}

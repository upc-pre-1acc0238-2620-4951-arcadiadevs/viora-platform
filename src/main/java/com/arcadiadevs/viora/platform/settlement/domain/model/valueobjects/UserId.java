package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of the producer who owns the plot and settles its harvests.
 *
 * @param userId UUID string
 */
public record UserId(String userId) {

    public UserId {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("settlement.user.id.null_or_empty");
        }
        try {
            userId = UUID.fromString(userId.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("settlement.user.id.invalid_uuid", e);
        }
    }
}

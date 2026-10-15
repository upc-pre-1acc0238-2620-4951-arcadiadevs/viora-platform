package com.arcadiadevs.viora.platform.iam.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when a password reset token has been consumed and the password reset.
 *
 * @param userId the user identifier string
 * @param occurredOn event timestamp
 */
public record PasswordResetCompletedEvent(
        String userId,
        Instant occurredOn
) {
    public PasswordResetCompletedEvent {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user.event.user_id.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("user.event.occurred_on.null");
        }
    }
}

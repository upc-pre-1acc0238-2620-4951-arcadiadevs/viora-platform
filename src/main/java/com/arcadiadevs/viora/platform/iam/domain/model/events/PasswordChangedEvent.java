package com.arcadiadevs.viora.platform.iam.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when an authenticated user updates their password.
 *
 * @param userId the user identifier string
 * @param occurredOn timestamp of password update
 */
public record PasswordChangedEvent(
        String userId,
        Instant occurredOn
) {
    public PasswordChangedEvent {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user.event.user_id.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("user.event.occurred_on.null");
        }
    }
}

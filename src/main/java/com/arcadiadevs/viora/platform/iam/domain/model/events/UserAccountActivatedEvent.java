package com.arcadiadevs.viora.platform.iam.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when a pending account verifies its email address and becomes active.
 *
 * @param userId     the user identifier string
 * @param email      the verified email address
 * @param occurredOn event timestamp
 */
public record UserAccountActivatedEvent(
        String userId,
        String email,
        Instant occurredOn
) {
    public UserAccountActivatedEvent {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user.event.user_id.null");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("user.event.email.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("user.event.occurred_on.null");
        }
    }
}

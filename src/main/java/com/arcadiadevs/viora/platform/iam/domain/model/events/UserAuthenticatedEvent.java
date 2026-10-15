package com.arcadiadevs.viora.platform.iam.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when credentials are successfully validated.
 *
 * @param userId the unique identifier string of the authenticated user
 * @param email the email address string
 * @param occurredOn timestamp of successful authentication
 */
public record UserAuthenticatedEvent(
        String userId,
        String email,
        Instant occurredOn
) {
    public UserAuthenticatedEvent {
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

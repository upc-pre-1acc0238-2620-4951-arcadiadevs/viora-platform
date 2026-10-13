package com.arcadiadevs.viora.platform.iam.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when a new user account is successfully registered.
 *
 * @param userId the unique identifier string of the user
 * @param email the registered email address string
 * @param role the assigned role string
 * @param occurredOn the timestamp when the account was registered
 */
public record UserAccountRegisteredEvent(
        String userId,
        String email,
        String role,
        Instant occurredOn
) {
    public UserAccountRegisteredEvent {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user.event.user_id.null");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("user.event.email.null");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("user.event.role.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("user.event.occurred_on.null");
        }
    }
}

package com.arcadiadevs.viora.platform.iam.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when a password recovery code is issued for an account.
 *
 * <p>It records the fact only: the plain code never travels inside a domain event. The application service
 * delivers the code to the user through the transactional email port.</p>
 *
 * @param userId     the user identifier string
 * @param email      the account email address
 * @param expiresAt  code expiration instant
 * @param occurredOn event timestamp
 */
public record PasswordResetRequestedEvent(
        String userId,
        String email,
        Instant expiresAt,
        Instant occurredOn
) {
    public PasswordResetRequestedEvent {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user.event.user_id.null");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("user.event.email.null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("user.event.expires_at.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("user.event.occurred_on.null");
        }
    }
}

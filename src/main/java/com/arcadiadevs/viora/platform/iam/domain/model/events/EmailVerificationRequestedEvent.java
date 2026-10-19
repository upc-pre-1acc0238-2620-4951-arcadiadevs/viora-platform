package com.arcadiadevs.viora.platform.iam.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when an email verification code is issued for a pending account.
 *
 * <p>The plain code never travels inside a domain event; the application service delivers it by email.</p>
 *
 * @param userId     the user identifier string
 * @param email      the account email address
 * @param expiresAt  code expiration instant
 * @param occurredOn event timestamp
 */
public record EmailVerificationRequestedEvent(
        String userId,
        String email,
        Instant expiresAt,
        Instant occurredOn
) {
    public EmailVerificationRequestedEvent {
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

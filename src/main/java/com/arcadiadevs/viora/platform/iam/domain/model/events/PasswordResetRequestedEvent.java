package com.arcadiadevs.viora.platform.iam.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when a password reset token is requested.
 * Consumed by asynchronous email notifications (e.g. Brevo).
 *
 * @param userId the user identifier string
 * @param email the recipient email address
 * @param resetToken the generated ephemeral recovery token string
 * @param expiresAt token expiration instant
 * @param occurredOn event timestamp
 */
public record PasswordResetRequestedEvent(
        String userId,
        String email,
        String resetToken,
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
        if (resetToken == null || resetToken.isBlank()) {
            throw new IllegalArgumentException("user.event.reset_token.null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("user.event.expires_at.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("user.event.occurred_on.null");
        }
    }
}

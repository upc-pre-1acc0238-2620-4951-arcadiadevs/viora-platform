package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Value object representing a rotating refresh token with a 7-day validity period.
 *
 * @param token the cryptographically unique opaque token
 * @param expiresAt expiration instant
 */
public record RefreshToken(String token, Instant expiresAt) {

    private static final long DEFAULT_VALIDITY_DAYS = 7;

    /**
     * Issues a new refresh token valid for 7 days.
     *
     * @return newly issued RefreshToken
     */
    public static RefreshToken issue() {
        return new RefreshToken(
                UUID.randomUUID().toString(),
                Instant.now().plus(DEFAULT_VALIDITY_DAYS, ChronoUnit.DAYS)
        );
    }

    /**
     * Compact constructor validating fields.
     *
     * @throws IllegalArgumentException if token or expiresAt is invalid
     */
    public RefreshToken {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("user.refresh_token.null_or_empty");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("user.refresh_token_expiry.null");
        }
        token = token.trim();
    }

    /**
     * Checks if this refresh token is expired.
     *
     * @param now current instant
     * @return true if expired; false otherwise
     */
    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}

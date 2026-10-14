package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Value object representing an ephemeral password recovery token (6-digit numeric OTP) with a 15-minute lifespan.
 *
 * @param token the cryptographically unique token or 6-digit OTP string
 * @param expiresAt the instant after which this token cannot be used
 */
public record PasswordResetToken(String token, Instant expiresAt) {

    private static final long EXPIRY_MINUTES = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Factory method creating a fresh reset token (6-digit OTP) valid for 15 minutes from now.
     *
     * @return newly issued PasswordResetToken
     */
    public static PasswordResetToken issue() {
        int otp = SECURE_RANDOM.nextInt(1_000_000);
        String code = String.format("%06d", otp);
        return new PasswordResetToken(code, Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES));
    }

    /**
     * Compact constructor validating that fields are non-null.
     *
     * @throws IllegalArgumentException if token or expiresAt is null
     */
    public PasswordResetToken {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("user.reset_token.null_or_empty");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("user.reset_token_expiry.null");
        }
        token = token.trim();
    }

    /**
     * Convenience constructor creating a token wrapper for lookup or verification purposes,
     * assigning an indefinite future expiration instant.
     *
     * @param token the cryptographically unique token string
     */
    public PasswordResetToken(String token) {
        this(token, Instant.MAX);
    }

    /**
     * Checks if this token is currently expired against the provided clock.
     *
     * @param now current instant
     * @return true if expired; false otherwise
     */
    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}

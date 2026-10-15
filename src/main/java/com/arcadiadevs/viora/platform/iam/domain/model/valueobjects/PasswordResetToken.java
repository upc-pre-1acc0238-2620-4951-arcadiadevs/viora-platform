package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.time.Duration;
import java.time.Instant;

/**
 * Value object representing the active password recovery code of an account, with a 15-minute lifespan.
 *
 * <p>Holds only the hash of the code, never the code itself. It also counts wrong codes submitted against it:
 * once {@link #MAX_FAILED_ATTEMPTS} is reached it can no longer be used and a new code must be requested.</p>
 *
 * @param codeHash       the hash of the recovery code
 * @param expiresAt      the instant after which the code cannot be used
 * @param failedAttempts number of wrong codes submitted against this token
 */
public record PasswordResetToken(String codeHash, Instant expiresAt, int failedAttempts) {

    /** Wrong codes tolerated before the token is locked. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    private static final Duration LIFESPAN = Duration.ofMinutes(15);

    /**
     * Compact constructor validating the token invariants.
     *
     * @throws IllegalArgumentException if the hash is missing, the expiry is null, or the attempt count is negative
     */
    public PasswordResetToken {
        if (codeHash == null || codeHash.isBlank()) {
            throw new IllegalArgumentException("user.reset_token.null_or_empty");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("user.reset_token_expiry.null");
        }
        if (failedAttempts < 0) {
            throw new IllegalArgumentException("user.reset_token.failed_attempts.negative");
        }
    }

    /**
     * Creates the token for a freshly hashed code, valid for 15 minutes from {@code issuedAt}.
     *
     * @param codeHash the hash of the issued code
     * @param issuedAt the instant the code is issued
     * @return a new token with no failed attempts
     * @throws IllegalArgumentException if the hash is missing or the instant is null
     */
    public static PasswordResetToken issue(String codeHash, Instant issuedAt) {
        if (issuedAt == null) {
            throw new IllegalArgumentException("user.clock.now.null");
        }
        return new PasswordResetToken(codeHash, issuedAt.plus(LIFESPAN), 0);
    }

    /**
     * Returns a copy of this token with one more failed attempt recorded.
     *
     * @return the updated token
     */
    public PasswordResetToken withFailedAttempt() {
        return new PasswordResetToken(codeHash, expiresAt, failedAttempts + 1);
    }

    /**
     * Checks whether too many wrong codes were submitted against this token.
     *
     * @return true if the token is locked
     */
    public boolean hasExhaustedAttempts() {
        return failedAttempts >= MAX_FAILED_ATTEMPTS;
    }

    /**
     * Checks if this token is currently expired against the provided instant.
     *
     * @param now current instant
     * @return true if expired; false otherwise
     */
    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}

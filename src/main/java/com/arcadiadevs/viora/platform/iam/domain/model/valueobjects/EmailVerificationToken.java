package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.time.Duration;
import java.time.Instant;

/**
 * Value object representing the active email verification code of a pending account.
 *
 * <p>Holds only the hash of the code. The code lasts 10 minutes, tolerates {@link #MAX_FAILED_ATTEMPTS} wrong
 * codes, and a new one can only be sent {@link #RESEND_COOLDOWN} after the previous one was issued.</p>
 *
 * @param codeHash       the hash of the verification code
 * @param issuedAt       the instant the code was issued
 * @param failedAttempts number of wrong codes submitted against this token
 */
public record EmailVerificationToken(String codeHash, Instant issuedAt, int failedAttempts) {

    /** Wrong codes tolerated before the token is locked. */
    public static final int MAX_FAILED_ATTEMPTS = 3;

    /** Minimum time between two codes sent to the same account. */
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(45);

    private static final Duration LIFESPAN = Duration.ofMinutes(10);

    /**
     * Compact constructor validating the token invariants.
     *
     * @throws IllegalArgumentException if the hash is missing, the issue instant is null, or the attempt count is
     *                                  negative
     */
    public EmailVerificationToken {
        if (codeHash == null || codeHash.isBlank()) {
            throw new IllegalArgumentException("user.email_verification.code_hash.null_or_empty");
        }
        if (issuedAt == null) {
            throw new IllegalArgumentException("user.email_verification.issued_at.null");
        }
        if (failedAttempts < 0) {
            throw new IllegalArgumentException("user.email_verification.failed_attempts.negative");
        }
    }

    /**
     * Creates the token for a freshly hashed code.
     *
     * @param codeHash the hash of the issued code
     * @param issuedAt the instant the code is issued
     * @return a new token with no failed attempts
     */
    public static EmailVerificationToken issue(String codeHash, Instant issuedAt) {
        return new EmailVerificationToken(codeHash, issuedAt, 0);
    }

    /**
     * Returns the instant after which the code cannot be used.
     *
     * @return the expiry instant
     */
    public Instant expiresAt() {
        return issuedAt.plus(LIFESPAN);
    }

    /**
     * Checks if this token is expired against the provided instant.
     *
     * @param now current instant
     * @return true if expired; false otherwise
     */
    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt());
    }

    /**
     * Returns how long the account still has to wait before a new code can be sent.
     *
     * @param now current instant
     * @return the remaining wait, or {@link Duration#ZERO} when a new code can be sent now
     */
    public Duration remainingCooldown(Instant now) {
        var remaining = Duration.between(now, issuedAt.plus(RESEND_COOLDOWN));
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Returns a copy of this token with one more failed attempt recorded.
     *
     * @return the updated token
     */
    public EmailVerificationToken withFailedAttempt() {
        return new EmailVerificationToken(codeHash, issuedAt, failedAttempts + 1);
    }

    /**
     * Returns how many wrong codes can still be submitted.
     *
     * @return the remaining attempts, never negative
     */
    public int remainingAttempts() {
        return Math.max(0, MAX_FAILED_ATTEMPTS - failedAttempts);
    }

    /**
     * Checks whether too many wrong codes were submitted against this token.
     *
     * @return true if the token is locked
     */
    public boolean hasExhaustedAttempts() {
        return failedAttempts >= MAX_FAILED_ATTEMPTS;
    }
}

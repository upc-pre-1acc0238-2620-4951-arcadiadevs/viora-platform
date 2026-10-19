package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.time.Duration;
import java.time.Instant;

/**
 * Value object representing the active password reset link of an account, valid for 30 minutes and single use.
 *
 * <p>Holds only the hash of the random token carried by the link, never the token itself. The token is long and
 * random, so it cannot be guessed and needs no attempt counter. Issuing a new link replaces this one.</p>
 *
 * @param tokenHash the hash of the token carried by the reset link
 * @param expiresAt the instant after which the link cannot be used
 */
public record PasswordResetToken(TokenHash tokenHash, Instant expiresAt) {

    private static final Duration LIFESPAN = Duration.ofMinutes(30);

    /**
     * Compact constructor validating the token invariants.
     *
     * @throws IllegalArgumentException if the hash is missing or the expiry is null
     */
    public PasswordResetToken {
        if (tokenHash == null) {
            throw new IllegalArgumentException("user.reset_token.null_or_empty");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("user.reset_token_expiry.null");
        }
    }

    /**
     * Creates the token for a freshly issued link, valid for 30 minutes from {@code issuedAt}.
     *
     * @param tokenHash the hash of the token carried by the link
     * @param issuedAt  the instant the link is issued
     * @return a new token
     * @throws IllegalArgumentException if the hash is missing or the instant is null
     */
    public static PasswordResetToken issue(TokenHash tokenHash, Instant issuedAt) {
        if (issuedAt == null) {
            throw new IllegalArgumentException("user.clock.now.null");
        }
        return new PasswordResetToken(tokenHash, issuedAt.plus(LIFESPAN));
    }

    /**
     * Checks whether a presented token hash is the one of this link, in constant time.
     *
     * @param presentedTokenHash hash of the token taken from the link
     * @return true if it is this link's token; false otherwise
     */
    public boolean matches(TokenHash presentedTokenHash) {
        return tokenHash.matches(presentedTokenHash);
    }

    /**
     * Checks whether the link is still usable at the given instant.
     *
     * @param now current instant
     * @return true if not expired; false otherwise
     */
    public boolean isUsableAt(Instant now) {
        return !now.isAfter(expiresAt);
    }
}

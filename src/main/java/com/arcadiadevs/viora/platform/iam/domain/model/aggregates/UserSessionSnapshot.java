package com.arcadiadevs.viora.platform.iam.domain.model.aggregates;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserSessionId;

import java.time.Instant;

/**
 * Immutable state snapshot of a {@link UserSession} aggregate root.
 *
 * @param id                 session identifier VO
 * @param userId             the account the session belongs to
 * @param refreshTokenHash   hash of the refresh token
 * @param credentialsVersion credentials version of the account when the session started
 * @param createdAt          start instant
 * @param expiresAt          instant after which the refresh token cannot be used
 * @param revokedAt          revocation instant, or null while the session is not revoked
 */
public record UserSessionSnapshot(
        UserSessionId id,
        UserId userId,
        String refreshTokenHash,
        long credentialsVersion,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt
) {
    public UserSessionSnapshot {
        if (id == null) {
            throw new IllegalArgumentException("user.session.snapshot.id.null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("user.session.snapshot.user_id.null");
        }
        if (refreshTokenHash == null || refreshTokenHash.isBlank()) {
            throw new IllegalArgumentException("user.session.snapshot.refresh_token_hash.null");
        }
        if (createdAt == null || expiresAt == null) {
            throw new IllegalArgumentException("user.session.snapshot.instants.null");
        }
    }
}

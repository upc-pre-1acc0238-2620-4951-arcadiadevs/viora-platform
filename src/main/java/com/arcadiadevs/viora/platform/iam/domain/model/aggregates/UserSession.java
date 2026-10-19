package com.arcadiadevs.viora.platform.iam.domain.model.aggregates;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserSessionId;
import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Duration;
import java.time.Instant;

/**
 * Pure Domain Aggregate Root representing a signed-in session of a user account, held by its refresh token.
 *
 * <p>It is a separate aggregate from {@link UserAccount} because an account accumulates sessions without limit; it
 * refers to the account by {@link UserId} only. The session keeps only the hash of its refresh token and the
 * credentials version of the account when it started. Each refresh rotates it: the old session is revoked and a new
 * one is started. Contains zero JPA, framework, or presentation annotations.</p>
 */
public class UserSession extends AbstractDomainAggregateRoot<UserSession> {

    /** How long a refresh token can be used. */
    public static final Duration LIFESPAN = Duration.ofDays(7);

    /**
     * How long a session is kept after it ends (expires or is revoked). Keeping it a while lets a reused, already
     * rotated refresh token still be recognized as stolen.
     */
    public static final Duration RETENTION_AFTER_END = Duration.ofDays(30);

    private final UserSessionId id;
    private final UserId userId;
    private final TokenHash refreshTokenHash;
    private final long credentialsVersion;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant revokedAt;

    private UserSession(UserSessionId id, UserId userId, TokenHash refreshTokenHash, long credentialsVersion,
                        Instant createdAt, Instant expiresAt, Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.credentialsVersion = credentialsVersion;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    /**
     * Factory method starting a session valid for {@link #LIFESPAN}.
     *
     * @param userId             the account the session belongs to
     * @param refreshTokenHash   hash of the refresh token handed to the client
     * @param credentialsVersion credentials version of the account when the session starts
     * @param now                the start instant
     * @return the new session
     * @throws IllegalArgumentException if an argument is missing or the version is negative
     */
    public static UserSession start(UserId userId, TokenHash refreshTokenHash, long credentialsVersion, Instant now) {
        if (userId == null) {
            throw new IllegalArgumentException("user.session.user_id.null");
        }
        if (refreshTokenHash == null) {
            throw new IllegalArgumentException("user.session.refresh_token_hash.null");
        }
        if (credentialsVersion < 0) {
            throw new IllegalArgumentException("user.session.credentials_version.negative");
        }
        if (now == null) {
            throw new IllegalArgumentException("user.clock.now.null");
        }
        return new UserSession(new UserSessionId(), userId, refreshTokenHash, credentialsVersion, now,
                now.plus(LIFESPAN), null);
    }

    /**
     * Reconstitutes an existing session from its persistence snapshot.
     *
     * @param snapshot immutable snapshot
     * @return reconstituted session
     */
    public static UserSession reconstitute(UserSessionSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("user.session.snapshot.null");
        }
        return new UserSession(snapshot.id(), snapshot.userId(), snapshot.refreshTokenHash(),
                snapshot.credentialsVersion(), snapshot.createdAt(), snapshot.expiresAt(), snapshot.revokedAt());
    }

    /**
     * Checks whether the session can still be used to refresh: not revoked and not expired.
     *
     * @param now current instant
     * @return true if the session is usable
     */
    public boolean isActive(Instant now) {
        return revokedAt == null && !now.isAfter(expiresAt);
    }

    /**
     * Revokes the session. Revoking an already revoked session keeps its original revocation instant.
     *
     * @param now the revocation instant
     * @throws IllegalArgumentException if {@code now} is null
     */
    public void revoke(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("user.clock.now.null");
        }
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    /**
     * Exposes the current state as an immutable snapshot.
     *
     * @return UserSessionSnapshot instance
     */
    public UserSessionSnapshot snapshot() {
        return new UserSessionSnapshot(id, userId, refreshTokenHash, credentialsVersion, createdAt, expiresAt,
                revokedAt);
    }
}

package com.arcadiadevs.viora.platform.iam.domain.repositories;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserSession;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Pure Domain Repository port for persisting and retrieving {@link UserSession} aggregates.
 *
 * <p>Contains zero Spring Data or infrastructure dependencies.</p>
 */
public interface UserSessionRepository {

    /**
     * Persists the current state of a session.
     *
     * @param session aggregate to save
     * @return persisted aggregate
     */
    UserSession save(UserSession session);

    /**
     * Finds the session holding the given refresh token hash, revoked, expired or not: the caller decides with
     * {@link UserSession#isActive}.
     *
     * @param refreshTokenHash hash of the refresh token
     * @return Optional containing the session if found
     */
    Optional<UserSession> findByRefreshTokenHash(TokenHash refreshTokenHash);

    /**
     * Finds the sessions of an account that are not revoked yet.
     *
     * @param userId the account identifier
     * @return the sessions that are not revoked, possibly empty
     */
    List<UserSession> findNotRevokedByUserId(UserId userId);
}

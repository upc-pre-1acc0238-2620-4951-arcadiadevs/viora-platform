package com.arcadiadevs.viora.platform.iam.domain.repositories;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserSession;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserSessionId;

import java.time.Instant;
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
     * Same as {@link #findByRefreshTokenHash}, holding the session until the transaction ends, so two refreshes of the
     * same token run one after the other.
     *
     * @param refreshTokenHash hash of the refresh token
     * @return Optional containing the locked session if found
     */
    Optional<UserSession> findByRefreshTokenHashForUpdate(TokenHash refreshTokenHash);

    /**
     * Finds a session by its identifier, holding it until the transaction ends.
     *
     * @param id the session identifier
     * @return Optional containing the locked session if found
     */
    Optional<UserSession> findByIdForUpdate(UserSessionId id);

    /**
     * Finds the sessions of an account that are not revoked yet.
     *
     * @param userId the account identifier
     * @return the sessions that are not revoked, possibly empty
     */
    List<UserSession> findNotRevokedByUserId(UserId userId);

    /**
     * Removes the sessions that expired or were revoked before the given instant.
     *
     * @param cutoff sessions that ended before this instant are removed
     * @return how many sessions were removed
     */
    int deleteEndedBefore(Instant cutoff);
}

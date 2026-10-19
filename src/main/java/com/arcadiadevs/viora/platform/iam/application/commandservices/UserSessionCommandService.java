package com.arcadiadevs.viora.platform.iam.application.commandservices;

import com.arcadiadevs.viora.platform.iam.domain.model.commands.AuthenticateUserCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.OpenUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.PurgeEndedUserSessionsCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RefreshUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.SignOutUserSessionCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/**
 * Application service port for the use cases that open and renew sessions.
 *
 * <p>They answer an {@link AuthenticatedSession} instead of an identifier because the tokens are the outcome of the
 * use case.</p>
 */
public interface UserSessionCommandService {

    /**
     * Signs in with email and password.
     *
     * @param command sign-in command
     * @return the new session, or why the credentials were rejected
     */
    Result<AuthenticatedSession, ApplicationError> handle(AuthenticateUserCommand command);

    /**
     * Opens a session for an active account, such as one that just verified its email.
     *
     * @param command open session command
     * @return the new session, or why it cannot be opened
     */
    Result<AuthenticatedSession, ApplicationError> handle(OpenUserSessionCommand command);

    /**
     * Rotates a refresh token: revokes its session and opens a new one.
     *
     * @param command refresh command
     * @return the new session, or why the refresh token was rejected
     */
    Result<AuthenticatedSession, ApplicationError> handle(RefreshUserSessionCommand command);

    /**
     * Removes the sessions that ended longer ago than their retention.
     *
     * @param command purge command
     * @return how many sessions were removed
     */
    Result<Integer, ApplicationError> handle(PurgeEndedUserSessionsCommand command);

    /**
     * Signs out: closes the session held by the refresh token. Signing out twice, or with an unknown token, is
     * not an error.
     *
     * @param command sign-out command
     * @return empty success
     */
    Result<Void, ApplicationError> handle(SignOutUserSessionCommand command);
}

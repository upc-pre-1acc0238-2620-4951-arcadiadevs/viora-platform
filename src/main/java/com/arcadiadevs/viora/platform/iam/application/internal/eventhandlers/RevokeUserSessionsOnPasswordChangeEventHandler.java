package com.arcadiadevs.viora.platform.iam.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordChangedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetCompletedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserSessionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Closes every session of an account when its password changes or is recovered.
 *
 * <p>It runs inside the transaction that changed the password, so the password and the revocation commit together.
 * The access tokens already issued stop working too, because they carry the old credentials version.</p>
 */
@Component
public class RevokeUserSessionsOnPasswordChangeEventHandler {

    private final UserSessionRepository userSessionRepository;

    /**
     * Creates the handler.
     *
     * @param userSessionRepository the user session repository port
     * @throws IllegalArgumentException if the repository is null
     */
    public RevokeUserSessionsOnPasswordChangeEventHandler(UserSessionRepository userSessionRepository) {
        if (userSessionRepository == null) {
            throw new IllegalArgumentException("user.session.repository.null");
        }
        this.userSessionRepository = userSessionRepository;
    }

    /**
     * Revokes the sessions of an account whose password was changed.
     *
     * @param event the password change
     */
    @EventListener
    public void on(PasswordChangedEvent event) {
        revokeAll(event.userId(), event.occurredOn());
    }

    /**
     * Revokes the sessions of an account whose password was recovered.
     *
     * @param event the completed password reset
     */
    @EventListener
    public void on(PasswordResetCompletedEvent event) {
        revokeAll(event.userId(), event.occurredOn());
    }

    private void revokeAll(String userId, Instant occurredOn) {
        for (var session : userSessionRepository.findNotRevokedByUserId(new UserId(userId))) {
            session.revoke(occurredOn);
            userSessionRepository.save(session);
        }
    }
}

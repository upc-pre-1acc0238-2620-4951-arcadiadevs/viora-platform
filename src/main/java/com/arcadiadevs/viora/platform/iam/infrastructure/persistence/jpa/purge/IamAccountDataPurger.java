package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.purge;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.repositories.UserSessionPersistenceRepository;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Removes every session of a deleted account, so no refresh token of it can be renewed again.
 */
@Component
public class IamAccountDataPurger {

    private final UserSessionPersistenceRepository sessions;

    /**
     * Creates the purger.
     *
     * @param sessions the sessions table
     */
    public IamAccountDataPurger(UserSessionPersistenceRepository sessions) {
        this.sessions = sessions;
    }

    /**
     * Deletes the sessions of the account.
     *
     * @param event the deleted account
     */
    @EventListener
    public void on(UserAccountDeletedEvent event) {
        sessions.deleteByUserId(UUID.fromString(event.userId()));
    }
}

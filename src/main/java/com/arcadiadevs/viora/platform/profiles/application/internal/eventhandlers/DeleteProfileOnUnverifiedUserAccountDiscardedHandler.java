package com.arcadiadevs.viora.platform.profiles.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UnverifiedUserAccountDiscardedEvent;
import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.DeleteProfileCommand;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event handler in Profiles removing the profile of an account that IAM discarded because it never verified its email.
 *
 * <p>It runs inside the transaction that discards the account, so the account and its profile disappear together.</p>
 */
@Component
public class DeleteProfileOnUnverifiedUserAccountDiscardedHandler {

    private final ProfileCommandService profileCommandService;

    /**
     * Creates the handler.
     *
     * @param profileCommandService the profile command service
     * @throws IllegalArgumentException if the service is null
     */
    public DeleteProfileOnUnverifiedUserAccountDiscardedHandler(ProfileCommandService profileCommandService) {
        if (profileCommandService == null) {
            throw new IllegalArgumentException("profile.command_service.null");
        }
        this.profileCommandService = profileCommandService;
    }

    /**
     * Removes the profile of the discarded account. An account that never had a profile is ignored.
     *
     * @param event the discarded account
     */
    @EventListener
    public void on(UnverifiedUserAccountDiscardedEvent event) {
        if (event == null) {
            return;
        }
        profileCommandService.handle(new DeleteProfileCommand(event.userId()));
    }
}

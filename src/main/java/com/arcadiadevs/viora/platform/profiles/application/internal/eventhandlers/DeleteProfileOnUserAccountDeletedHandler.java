package com.arcadiadevs.viora.platform.profiles.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.DeleteProfileCommand;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event handler in Profiles removing the profile of an account its owner deleted (P100).
 *
 * <p>It runs inside the transaction that deletes the account, so the account and its profile disappear together.</p>
 */
@Component
public class DeleteProfileOnUserAccountDeletedHandler {

    private final ProfileCommandService profileCommandService;

    /**
     * Creates the handler.
     *
     * @param profileCommandService the profile command service
     * @throws IllegalArgumentException if the service is null
     */
    public DeleteProfileOnUserAccountDeletedHandler(ProfileCommandService profileCommandService) {
        if (profileCommandService == null) {
            throw new IllegalArgumentException("profile.command_service.null");
        }
        this.profileCommandService = profileCommandService;
    }

    /**
     * Removes the profile of the deleted account. An account that never had a profile is ignored.
     *
     * @param event the deleted account
     */
    @EventListener
    public void on(UserAccountDeletedEvent event) {
        if (event == null) {
            return;
        }
        profileCommandService.handle(new DeleteProfileCommand(event.userId()));
    }
}

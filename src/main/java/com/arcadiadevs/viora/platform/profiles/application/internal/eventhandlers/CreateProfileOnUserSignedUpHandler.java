package com.arcadiadevs.viora.platform.profiles.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserSignedUpWithProfileEvent;
import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event handler in Profiles listening to IAM registration events to automatically create user profiles.
 */
@Component
public class CreateProfileOnUserSignedUpHandler {

    private final ProfileCommandService profileCommandService;

    public CreateProfileOnUserSignedUpHandler(ProfileCommandService profileCommandService) {
        if (profileCommandService == null) {
            throw new IllegalArgumentException("profile.command_service.null");
        }
        this.profileCommandService = profileCommandService;
    }

    @EventListener
    public void on(UserSignedUpWithProfileEvent event) {
        if (event == null) {
            return;
        }
        var command = new CreateProfileCommand(
                event.userId(),
                event.fullName(),
                event.country(),
                event.phoneNumber()
        );
        var result = profileCommandService.handle(command);
        if (result instanceof Result.Failure<String, ApplicationError> failure) {
            throw new BusinessRuleException(failure.error().message());
        }
    }
}

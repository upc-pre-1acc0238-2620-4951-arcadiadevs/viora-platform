package com.arcadiadevs.viora.platform.profiles.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserSignedUpWithProfileEvent;
import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.application.queryservices.ProfileQueryService;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.UpdateContactProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event handler in Profiles listening to IAM sign-up events to keep the user profile in step with the sign-up.
 *
 * <p>The first sign-up creates the profile. A pending account that signs up again raises the event once more, and
 * then the existing profile is updated with the new details instead of failing as a duplicate.</p>
 */
@Component
public class CreateProfileOnUserSignedUpHandler {

    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;

    /**
     * Creates the handler.
     *
     * @param profileCommandService the profile command service
     * @param profileQueryService   the profile query service, used to tell a first sign-up from a repeated one
     * @throws IllegalArgumentException if any dependency is null
     */
    public CreateProfileOnUserSignedUpHandler(ProfileCommandService profileCommandService,
                                              ProfileQueryService profileQueryService) {
        if (profileCommandService == null) {
            throw new IllegalArgumentException("profile.command_service.null");
        }
        if (profileQueryService == null) {
            throw new IllegalArgumentException("profile.query_service.null");
        }
        this.profileCommandService = profileCommandService;
        this.profileQueryService = profileQueryService;
    }

    @EventListener
    public void on(UserSignedUpWithProfileEvent event) {
        if (event == null) {
            return;
        }
        var profileExists = profileQueryService.handle(new GetProfileByUserIdQuery(event.userId())).isSuccess();
        if (profileExists) {
            var result = profileCommandService.handle(new UpdateContactProfileCommand(
                    event.userId(), event.fullName(), event.country(), event.phoneNumber()));
            if (result instanceof Result.Failure<?, ApplicationError> failure) {
                throw new BusinessRuleException(failure.error().message());
            }
            return;
        }
        var result = profileCommandService.handle(new CreateProfileCommand(
                event.userId(), event.fullName(), event.country(), event.phoneNumber()));
        if (result instanceof Result.Failure<?, ApplicationError> failure) {
            throw new BusinessRuleException(failure.error().message());
        }
    }
}

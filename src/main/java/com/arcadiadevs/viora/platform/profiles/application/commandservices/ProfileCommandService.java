package com.arcadiadevs.viora.platform.profiles.application.commandservices;

import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.DeleteProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.UpdateContactProfileCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/**
 * Application service port for mutating commands on user profiles.
 */
public interface ProfileCommandService {

    /**
     * Handles the formal creation of a user profile.
     *
     * @param command creation command
     * @return created profile identifier or application error
     */
    Result<String, ApplicationError> handle(CreateProfileCommand command);

    /**
     * Handles updating contact details on an existing profile.
     *
     * @param command contact update command
     * @return updated Profile aggregate or application error
     */
    Result<Profile, ApplicationError> handle(UpdateContactProfileCommand command);

    /**
     * Removes the profile of a user account, if it has one.
     *
     * @param command delete command
     * @return true if a profile was removed, false if the account had none
     */
    Result<Boolean, ApplicationError> handle(DeleteProfileCommand command);
}

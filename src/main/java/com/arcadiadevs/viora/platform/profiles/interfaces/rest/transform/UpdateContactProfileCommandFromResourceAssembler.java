package com.arcadiadevs.viora.platform.profiles.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.profiles.domain.model.commands.UpdateContactProfileCommand;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.resources.UpdateProfileResource;

/**
 * Assembler creating {@link UpdateContactProfileCommand} from path userId and {@link UpdateProfileResource}.
 */
public final class UpdateContactProfileCommandFromResourceAssembler {

    private UpdateContactProfileCommandFromResourceAssembler() {
    }

    public static UpdateContactProfileCommand toCommandFromResource(String userId, UpdateProfileResource resource) {
        if (resource == null) {
            return null;
        }
        return new UpdateContactProfileCommand(
                userId,
                resource.fullName(),
                resource.country(),
                resource.phoneNumber()
        );
    }
}

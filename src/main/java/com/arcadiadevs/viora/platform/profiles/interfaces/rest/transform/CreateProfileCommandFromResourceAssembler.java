package com.arcadiadevs.viora.platform.profiles.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.resources.CreateProfileResource;

/**
 * Assembler creating {@link CreateProfileCommand} from {@link CreateProfileResource}.
 */
public final class CreateProfileCommandFromResourceAssembler {

    private CreateProfileCommandFromResourceAssembler() {
    }

    public static CreateProfileCommand toCommandFromResource(CreateProfileResource resource) {
        if (resource == null) {
            return null;
        }
        return new CreateProfileCommand(
                resource.userId(),
                resource.fullName(),
                resource.country(),
                resource.phoneNumber()
        );
    }
}

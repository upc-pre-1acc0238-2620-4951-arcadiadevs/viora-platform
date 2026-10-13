package com.arcadiadevs.viora.platform.iam.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.interfaces.rest.resources.SignUpResource;

/**
 * Assembler transforming {@link SignUpResource} into {@link RegisterUserAccountCommand}.
 */
public final class RegisterUserAccountCommandFromResourceAssembler {

    private RegisterUserAccountCommandFromResourceAssembler() {
    }

    public static RegisterUserAccountCommand toCommandFromResource(SignUpResource resource, String encodedPassword) {
        if (resource == null) {
            return null;
        }
        return new RegisterUserAccountCommand(
                resource.email(),
                encodedPassword,
                resource.role()
        );
    }
}

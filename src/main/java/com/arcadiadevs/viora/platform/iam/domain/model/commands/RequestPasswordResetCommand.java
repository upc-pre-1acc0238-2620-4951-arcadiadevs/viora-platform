package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for requesting a password reset email token.
 *
 * @param email target account email address
 */
public record RequestPasswordResetCommand(
        String email
) {
    public RequestPasswordResetCommand {
        if (email == null) {
            throw new IllegalArgumentException("user.command.email.null");
        }
    }
}

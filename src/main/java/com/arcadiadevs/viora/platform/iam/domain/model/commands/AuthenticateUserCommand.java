package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for authenticating user credentials.
 *
 * @param email user email string
 * @param rawPassword plain text password attempt
 */
public record AuthenticateUserCommand(
        String email,
        String rawPassword
) {
    public AuthenticateUserCommand {
        if (email == null) {
            throw new IllegalArgumentException("user.command.email.null");
        }
        if (rawPassword == null) {
            throw new IllegalArgumentException("user.command.raw_password.null");
        }
    }
}

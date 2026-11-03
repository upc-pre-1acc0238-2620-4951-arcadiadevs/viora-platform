package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for authenticating user credentials.
 *
 * @param email user email string
 * @param rawPassword plain text password attempt
 * @param role role the client serves, or null when it serves any
 */
public record AuthenticateUserCommand(
        String email,
        String rawPassword,
        String role
) {
    /**
     * Creates the command for a client that serves any role.
     *
     * @param email user email string
     * @param rawPassword plain text password attempt
     */
    public AuthenticateUserCommand(String email, String rawPassword) {
        this(email, rawPassword, null);
    }

    public AuthenticateUserCommand {
        if (email == null) {
            throw new IllegalArgumentException("user.command.email.null");
        }
        if (rawPassword == null) {
            throw new IllegalArgumentException("user.command.raw_password.null");
        }
    }
}

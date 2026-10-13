package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for registering a new user account.
 *
 * @param email raw email string
 * @param passwordHash derived password hash string
 * @param role target role string
 */
public record RegisterUserAccountCommand(
        String email,
        String passwordHash,
        String role
) {
    public RegisterUserAccountCommand {
        if (email == null) {
            throw new IllegalArgumentException("user.command.email.null");
        }
        if (passwordHash == null) {
            throw new IllegalArgumentException("user.command.password_hash.null");
        }
        if (role == null) {
            throw new IllegalArgumentException("user.command.role.null");
        }
    }
}

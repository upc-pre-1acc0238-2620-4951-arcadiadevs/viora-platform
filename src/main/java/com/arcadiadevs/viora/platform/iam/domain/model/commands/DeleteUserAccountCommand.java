package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for the producer deleting their own account for good (P100).
 *
 * @param userId      the user identifier string
 * @param rawPassword the current password, typed again to confirm
 */
public record DeleteUserAccountCommand(
        String userId,
        String rawPassword
) {
    public DeleteUserAccountCommand {
        if (userId == null) {
            throw new IllegalArgumentException("user.command.user_id.null");
        }
        if (rawPassword == null) {
            throw new IllegalArgumentException("user.command.current_password.null");
        }
    }
}

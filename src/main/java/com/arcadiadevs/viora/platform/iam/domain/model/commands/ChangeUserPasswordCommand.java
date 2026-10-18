package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for voluntarily changing an account password.
 *
 * @param userId             the user identifier string
 * @param currentRawPassword the current password provided by the user
 * @param newPassword        the new plain password
 */
public record ChangeUserPasswordCommand(
        String userId,
        String currentRawPassword,
        String newPassword
) {
    public ChangeUserPasswordCommand {
        if (userId == null) {
            throw new IllegalArgumentException("user.command.user_id.null");
        }
        if (currentRawPassword == null) {
            throw new IllegalArgumentException("user.command.current_password.null");
        }
        if (newPassword == null) {
            throw new IllegalArgumentException("user.command.new_password.null");
        }
    }
}

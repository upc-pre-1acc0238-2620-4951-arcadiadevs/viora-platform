package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for voluntarily changing an account password.
 *
 * @param userId the user identifier string
 * @param currentRawPassword the current password provided by the user
 * @param newPasswordHash the pre-hashed new password
 */
public record ChangeUserPasswordCommand(
        String userId,
        String currentRawPassword,
        String newPasswordHash
) {
    public ChangeUserPasswordCommand {
        if (userId == null) {
            throw new IllegalArgumentException("user.command.user_id.null");
        }
        if (currentRawPassword == null) {
            throw new IllegalArgumentException("user.command.current_password.null");
        }
        if (newPasswordHash == null) {
            throw new IllegalArgumentException("user.command.new_password_hash.null");
        }
    }
}

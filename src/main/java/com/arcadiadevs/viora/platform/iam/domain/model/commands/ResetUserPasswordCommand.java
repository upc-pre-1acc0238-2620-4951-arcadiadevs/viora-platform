package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for consuming a reset token and setting a new password.
 *
 * @param resetToken the recovery token string
 * @param newPasswordHash the pre-hashed new password
 */
public record ResetUserPasswordCommand(
        String resetToken,
        String newPasswordHash
) {
    public ResetUserPasswordCommand {
        if (resetToken == null) {
            throw new IllegalArgumentException("user.command.reset_token.null");
        }
        if (newPasswordHash == null) {
            throw new IllegalArgumentException("user.command.new_password_hash.null");
        }
    }
}

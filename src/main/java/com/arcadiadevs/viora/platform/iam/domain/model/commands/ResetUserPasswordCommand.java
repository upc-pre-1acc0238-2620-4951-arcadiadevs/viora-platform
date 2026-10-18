package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for consuming a password reset link and setting a new password.
 *
 * @param resetToken  the token carried by the reset link
 * @param newPassword the new plain password
 */
public record ResetUserPasswordCommand(
        String resetToken,
        String newPassword
) {
    public ResetUserPasswordCommand {
        if (resetToken == null) {
            throw new IllegalArgumentException("user.command.reset_token.null");
        }
        if (newPassword == null) {
            throw new IllegalArgumentException("user.command.new_password.null");
        }
    }
}

package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for consuming a recovery code and setting a new password.
 *
 * @param email           the email address of the account being recovered
 * @param resetToken      the 6-digit recovery code
 * @param newPasswordHash the pre-hashed new password
 */
public record ResetUserPasswordCommand(
        String email,
        String resetToken,
        String newPasswordHash
) {
    public ResetUserPasswordCommand {
        if (email == null) {
            throw new IllegalArgumentException("user.command.email.null");
        }
        if (resetToken == null) {
            throw new IllegalArgumentException("user.command.reset_token.null");
        }
        if (newPasswordHash == null) {
            throw new IllegalArgumentException("user.command.new_password_hash.null");
        }
    }
}

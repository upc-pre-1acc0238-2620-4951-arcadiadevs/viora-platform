package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for verifying the email address of a pending account with the code sent to it.
 *
 * @param email the email address being verified
 * @param code  the 6-digit verification code
 */
public record VerifyEmailCommand(String email, String code) {
    public VerifyEmailCommand {
        if (email == null) {
            throw new IllegalArgumentException("user.command.email.null");
        }
        if (code == null) {
            throw new IllegalArgumentException("user.command.verification_code.null");
        }
    }
}

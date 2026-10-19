package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for sending a new email verification code to a pending account.
 *
 * @param email the email address of the pending account
 */
public record ResendEmailVerificationCommand(String email) {
    public ResendEmailVerificationCommand {
        if (email == null) {
            throw new IllegalArgumentException("user.command.email.null");
        }
    }
}

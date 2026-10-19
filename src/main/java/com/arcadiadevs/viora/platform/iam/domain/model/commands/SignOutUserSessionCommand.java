package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for signing out: it closes the session held by a refresh token.
 *
 * @param refreshToken the refresh token of the session to close
 */
public record SignOutUserSessionCommand(String refreshToken) {
    public SignOutUserSessionCommand {
        if (refreshToken == null) {
            throw new IllegalArgumentException("user.command.refresh_token.null");
        }
    }
}

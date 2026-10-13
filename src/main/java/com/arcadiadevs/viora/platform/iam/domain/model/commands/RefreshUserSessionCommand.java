package com.arcadiadevs.viora.platform.iam.domain.model.commands;

/**
 * Domain command for renewing access tokens using an active refresh token.
 *
 * @param refreshToken the refresh token string
 */
public record RefreshUserSessionCommand(
        String refreshToken
) {
    public RefreshUserSessionCommand {
        if (refreshToken == null) {
            throw new IllegalArgumentException("user.command.refresh_token.null");
        }
    }
}

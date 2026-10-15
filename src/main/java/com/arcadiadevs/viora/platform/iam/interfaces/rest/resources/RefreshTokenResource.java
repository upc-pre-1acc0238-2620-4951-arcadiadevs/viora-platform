package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

/**
 * Resource payload for session token renewal.
 */
public record RefreshTokenResource(
        @NotBlank(message = "user.refresh_token.blank")
        String refreshToken
) {
}

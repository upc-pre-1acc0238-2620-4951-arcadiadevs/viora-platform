package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

/**
 * Resource payload for changing password with an active session.
 */
public record ChangePasswordResource(
        @NotBlank(message = "user.current_password.blank")
        String currentPassword,

        @NotBlank(message = "user.new_password.blank")
        String newPassword
) {
}

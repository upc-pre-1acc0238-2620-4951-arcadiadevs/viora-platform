package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resource payload for changing password with an active session.
 */
public record ChangePasswordResource(
        @NotBlank(message = "user.current_password.blank")
        String currentPassword,

        @NotBlank(message = "user.new_password.blank")
        @Size(min = 8, message = "user.new_password.min_length")
        String newPassword
) {
}

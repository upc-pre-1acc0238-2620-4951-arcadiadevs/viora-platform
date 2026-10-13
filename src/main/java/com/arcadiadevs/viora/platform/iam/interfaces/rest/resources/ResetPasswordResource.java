package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resource payload for setting a new password using a recovery token.
 */
public record ResetPasswordResource(
        @NotBlank(message = "user.new_password.blank")
        @Size(min = 8, message = "user.new_password.min_length")
        String newPassword
) {
}

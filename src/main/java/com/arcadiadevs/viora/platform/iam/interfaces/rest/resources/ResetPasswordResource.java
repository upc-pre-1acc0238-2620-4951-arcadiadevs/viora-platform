package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

/**
 * Resource payload for setting a new password with a password reset link. The token travels in the URL.
 *
 * @param newPassword the new plain password
 */
public record ResetPasswordResource(
        @NotBlank(message = "user.new_password.blank")
        String newPassword
) {
}

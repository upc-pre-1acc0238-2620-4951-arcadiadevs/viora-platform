package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resource payload for setting a new password using a recovery code.
 *
 * @param email       the email address the recovery code was sent to
 * @param newPassword the new plain password
 */
public record ResetPasswordResource(
        @NotBlank(message = "user.email.blank")
        @Email(message = "user.email.invalid")
        String email,
        @NotBlank(message = "user.new_password.blank")
        @Size(min = 8, message = "user.new_password.min_length")
        String newPassword
) {
}

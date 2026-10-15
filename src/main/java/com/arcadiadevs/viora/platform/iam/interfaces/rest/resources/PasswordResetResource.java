package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Resource payload for initiating a password recovery flow.
 */
public record PasswordResetResource(
        @NotBlank(message = "user.email.blank")
        @Email(message = "user.email.invalid")
        String email
) {
}

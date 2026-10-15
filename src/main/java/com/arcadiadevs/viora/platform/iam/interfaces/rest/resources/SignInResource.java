package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Resource payload for credentials authentication.
 */
public record SignInResource(
        @NotBlank(message = "user.email.blank")
        @Email(message = "user.email.invalid")
        String email,

        @NotBlank(message = "user.password.blank")
        String password
) {
}

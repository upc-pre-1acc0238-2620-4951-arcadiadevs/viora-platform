package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resource payload for user account registration, optionally containing profile data for mobile onboarding.
 */
public record SignUpResource(
        @NotBlank(message = "user.email.blank")
        @Email(message = "user.email.invalid")
        String email,

        @NotBlank(message = "user.password.blank")
        @Size(min = 8, message = "user.password.min_length")
        String password,

        @NotBlank(message = "user.role.blank")
        String role,

        @Valid
        ProfileDataResource profile
) {
    /**
     * Nested record containing optional profile data captured during mobile onboarding.
     */
    public record ProfileDataResource(
            @NotBlank(message = "profile.full_name.blank")
            String fullName,

            @NotBlank(message = "profile.country.blank")
            String country,

            @NotBlank(message = "profile.phone_number.blank")
            String phoneNumber
    ) {
    }
}

package com.arcadiadevs.viora.platform.profiles.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

/**
 * Resource payload for manually initializing a user profile.
 */
public record CreateProfileResource(
        @NotBlank(message = "profile.user_id.blank")
        String userId,

        @NotBlank(message = "profile.full_name.blank")
        String fullName,

        @NotBlank(message = "profile.country.blank")
        String country,

        @NotBlank(message = "profile.phone_number.blank")
        String phoneNumber
) {
}

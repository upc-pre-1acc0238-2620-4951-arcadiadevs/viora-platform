package com.arcadiadevs.viora.platform.profiles.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

/**
 * Resource payload for updating full name and contact information.
 */
public record UpdateProfileResource(
        @NotBlank(message = "profile.full_name.blank")
        String fullName,

        @NotBlank(message = "profile.country.blank")
        String country,

        @NotBlank(message = "profile.phone_number.blank")
        String phoneNumber
) {
}

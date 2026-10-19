package com.arcadiadevs.viora.platform.profiles.domain.model.commands;

/**
 * Domain command for updating user profile contact information.
 *
 * @param userId      the account whose profile is changed
 * @param fullName    raw full name
 * @param country     raw country code
 * @param phoneNumber raw phone number
 */
public record UpdateContactProfileCommand(
        String userId,
        String fullName,
        String country,
        String phoneNumber
) {
    public UpdateContactProfileCommand {
        if (userId == null) {
            throw new IllegalArgumentException("profile.command.user_id.null");
        }
        if (fullName == null) {
            throw new IllegalArgumentException("profile.command.full_name.null");
        }
        if (country == null) {
            throw new IllegalArgumentException("profile.command.country.null");
        }
        if (phoneNumber == null) {
            throw new IllegalArgumentException("profile.command.phone_number.null");
        }
    }
}

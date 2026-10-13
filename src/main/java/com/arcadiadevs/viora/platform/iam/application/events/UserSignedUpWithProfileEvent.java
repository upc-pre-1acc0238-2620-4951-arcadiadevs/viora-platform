package com.arcadiadevs.viora.platform.iam.application.events;

/**
 * Application event published when a user registers with onboarding profile details.
 * Consumed by the downstream Profiles bounded context to formalize user details.
 *
 * @param userId the newly created user account identifier
 * @param fullName full civil name
 * @param country country ISO alpha-2 code
 * @param phoneNumber telephone number in E.164 format
 */
public record UserSignedUpWithProfileEvent(
        String userId,
        String fullName,
        String country,
        String phoneNumber
) {
    public UserSignedUpWithProfileEvent {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user.event.user_id.null");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("user.event.full_name.null");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("user.event.country.null");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("user.event.phone_number.null");
        }
    }
}

package com.arcadiadevs.viora.platform.iam.domain.model.events;

import java.time.Instant;

/**
 * Domain event raised when a user account is registered together with onboarding profile details.
 *
 * <p>IAM does not keep these details; they travel in the event so the downstream Profiles bounded context can
 * create the profile. Raised by {@code UserAccount.register} and published by the command service.</p>
 *
 * @param userId the newly created user account identifier
 * @param fullName full civil name
 * @param country country ISO alpha-2 code
 * @param phoneNumber telephone number as submitted; Profiles normalizes it to E.164
 * @param occurredOn event timestamp
 */
public record UserSignedUpWithProfileEvent(
        String userId,
        String fullName,
        String country,
        String phoneNumber,
        Instant occurredOn
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
        if (occurredOn == null) {
            throw new IllegalArgumentException("user.event.occurred_on.null");
        }
    }
}

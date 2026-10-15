package com.arcadiadevs.viora.platform.profiles.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when profile contact details are updated.
 *
 * @param profileId profile identifier string
 * @param userId user identifier string
 * @param fullName updated full name
 * @param country updated country code
 * @param phoneNumber updated phone number
 * @param occurredOn event timestamp
 */
public record ContactProfileUpdatedEvent(
        String profileId,
        String userId,
        String fullName,
        String country,
        String phoneNumber,
        Instant occurredOn
) {
    public ContactProfileUpdatedEvent {
        if (profileId == null || profileId.isBlank()) {
            throw new IllegalArgumentException("profile.event.profile_id.null");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("profile.event.user_id.null");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("profile.event.full_name.null");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("profile.event.country.null");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("profile.event.phone_number.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("profile.event.occurred_on.null");
        }
    }
}

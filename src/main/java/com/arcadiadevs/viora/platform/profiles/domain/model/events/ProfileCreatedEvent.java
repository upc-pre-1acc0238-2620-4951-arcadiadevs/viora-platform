package com.arcadiadevs.viora.platform.profiles.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when a user profile is created and formalized.
 *
 * @param profileId unique profile identifier string
 * @param userId referenced user account identifier string
 * @param fullName full name of the user
 * @param occurredOn event timestamp
 */
public record ProfileCreatedEvent(
        String profileId,
        String userId,
        String fullName,
        Instant occurredOn
) {
    public ProfileCreatedEvent {
        if (profileId == null || profileId.isBlank()) {
            throw new IllegalArgumentException("profile.event.profile_id.null");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("profile.event.user_id.null");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("profile.event.full_name.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("profile.event.occurred_on.null");
        }
    }
}

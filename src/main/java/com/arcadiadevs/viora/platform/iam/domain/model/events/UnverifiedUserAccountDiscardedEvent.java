package com.arcadiadevs.viora.platform.iam.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when a pending account that never verified its email is discarded.
 *
 * <p>Other contexts react to it to remove what they created for that account, such as the profile.</p>
 *
 * @param userId     the discarded account identifier
 * @param email      the email address the account was registered with
 * @param occurredOn event timestamp
 */
public record UnverifiedUserAccountDiscardedEvent(
        String userId,
        String email,
        Instant occurredOn
) {
    public UnverifiedUserAccountDiscardedEvent {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user.event.user_id.null");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("user.event.email.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("user.event.occurred_on.null");
        }
    }
}

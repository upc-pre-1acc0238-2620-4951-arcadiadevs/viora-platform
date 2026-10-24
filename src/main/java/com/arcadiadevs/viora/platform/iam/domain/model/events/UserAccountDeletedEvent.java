package com.arcadiadevs.viora.platform.iam.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when a producer deletes their own account.
 *
 * <p>Every context that keeps data of the account reacts to it, in the same transaction, by removing that data for
 * good: profile, plots and everything recorded on them, settlements, dossiers and subscriptions.</p>
 *
 * @param userId     the deleted account identifier, which is also the producer identifier
 * @param email      the email address of the account
 * @param occurredOn event timestamp
 */
public record UserAccountDeletedEvent(
        String userId,
        String email,
        Instant occurredOn
) {
    public UserAccountDeletedEvent {
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

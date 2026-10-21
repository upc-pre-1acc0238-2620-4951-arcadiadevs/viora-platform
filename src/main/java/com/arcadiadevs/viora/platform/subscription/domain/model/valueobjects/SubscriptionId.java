package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object identifying a subscription.
 * <p>The identifier is stored as a UUID string. It throws an IllegalArgumentException if it is null, blank, or not a
 * valid UUID.</p>
 *
 * @param subscriptionId the identifier; it must be a valid UUID string
 */
public record SubscriptionId(String subscriptionId) {

    /**
     * Generates a new random identifier.
     */
    public SubscriptionId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public SubscriptionId {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("subscription.id.null_or_empty");
        }
        try {
            subscriptionId = UUID.fromString(subscriptionId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("subscription.id.invalid_uuid", exception);
        }
    }
}

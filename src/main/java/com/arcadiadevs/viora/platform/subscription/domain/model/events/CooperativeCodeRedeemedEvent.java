package com.arcadiadevs.viora.platform.subscription.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when a producer redeems a cooperative invitation code and gets a sponsored subscription.
 * Territory is meant to react to it to add the producer to the cooperative's roster.
 *
 * @param subscriptionId the sponsored subscription that was activated
 * @param producerId     the producer who redeemed the code
 * @param cooperativeId  the sponsoring cooperative
 * @param codeId         the redeemed code
 * @param occurredOn     when the code was redeemed
 */
public record CooperativeCodeRedeemedEvent(
        String subscriptionId,
        String producerId,
        String cooperativeId,
        String codeId,
        Instant occurredOn
) {

    /**
     * Compact constructor enforcing non-null event fields.
     */
    public CooperativeCodeRedeemedEvent {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("subscription.event.subscription_id.null");
        }
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("subscription.event.producer_id.null");
        }
        if (cooperativeId == null || cooperativeId.isBlank()) {
            throw new IllegalArgumentException("subscription.event.cooperative_id.null");
        }
        if (codeId == null || codeId.isBlank()) {
            throw new IllegalArgumentException("subscription.event.code_id.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("subscription.event.occurred_on.null");
        }
    }
}

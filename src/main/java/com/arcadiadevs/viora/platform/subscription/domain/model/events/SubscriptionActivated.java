package com.arcadiadevs.viora.platform.subscription.domain.model.events;

import java.time.Instant;

/**
 * Verified activation of an individually paid contract.
 * @param subscriptionId contract identity
 * @param producerId entitled producer
 * @param startsAt approved start instant
 * @param endsAt exclusive calendar-year end
 */
public record SubscriptionActivated(String subscriptionId, String producerId, Instant startsAt, Instant endsAt) { }

package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;

import java.time.Instant;

/**
 * Repository port of the subscriptions.
 */
public interface SubscriptionRepository {

    /**
     * Checks whether a producer holds a subscription that is active and in force at an instant.
     *
     * @param producerId the producer
     * @param at         the instant to check
     * @return true if such a subscription exists
     */
    boolean existsActiveByProducerId(ProducerId producerId, Instant at);

    /**
     * Stores a subscription.
     *
     * @param subscription the subscription to store
     * @return the stored subscription
     */
    Subscription save(Subscription subscription);
}

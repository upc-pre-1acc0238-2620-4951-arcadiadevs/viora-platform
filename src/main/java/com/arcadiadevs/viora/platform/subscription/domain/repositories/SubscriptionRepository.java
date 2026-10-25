package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;

import java.time.Instant;
import java.util.Optional;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;

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
    /**
     * Finds a contract using its domain identity.
     * @param id contract identity
     * @return stored contract when present
     */
    Optional<Subscription> findById(SubscriptionId id);

    /**
     * Finds the individual purchase still waiting for its payment.
     * @param producerId producer identity
     * @return the pending contract when present
     */
    Optional<Subscription> findPendingByProducerId(ProducerId producerId);

    /**
     * Resolves active access first, otherwise the latest owned contract.
     * @param producerId owner identity
     * @param at current instant
     * @return current contract when any exists
     */
    Optional<Subscription> findCurrent(ProducerId producerId, Instant at);


    /**
     * Looks up a contract without reading another producer's persistence row.
     * @param id contract identifier
     * @param producerId authenticated owner
     * @return owned contract, or empty if the identifier is missing or foreign
     */
    Optional<Subscription> findByIdAndProducerId(SubscriptionId id, ProducerId producerId);
}

package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.purge;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.PaymentAttemptPersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionGateRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionOperationRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionPersistenceRepository;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Removes the subscriptions of a deleted account with their payment attempts, idempotent operations and producer
 * gate. A redeemed invitation code stays redeemed: it is not handed out again.
 */
@Component
public class SubscriptionAccountDataPurger {

    private final SubscriptionPersistenceRepository subscriptions;
    private final PaymentAttemptPersistenceRepository attempts;
    private final SubscriptionOperationRepository operations;
    private final SubscriptionGateRepository gates;

    /**
     * Creates the purger.
     *
     * @param subscriptions the subscriptions table
     * @param attempts      the payment attempts table
     * @param operations    the idempotent operations table
     * @param gates         the per producer gates table
     */
    public SubscriptionAccountDataPurger(SubscriptionPersistenceRepository subscriptions,
                                         PaymentAttemptPersistenceRepository attempts,
                                         SubscriptionOperationRepository operations,
                                         SubscriptionGateRepository gates) {
        this.subscriptions = subscriptions;
        this.attempts = attempts;
        this.operations = operations;
        this.gates = gates;
    }

    /**
     * Removes the subscription data of the account.
     *
     * @param event the deleted account
     */
    @EventListener
    public void on(UserAccountDeletedEvent event) {
        attempts.deleteAll(attempts.findAllByProducerId(event.userId()));
        operations.deleteAll(operations.findAllByProducerId(event.userId()));
        subscriptions.deleteAll(subscriptions.findAllByProducerId(UUID.fromString(event.userId())));
        gates.findById(event.userId()).ifPresent(gates::delete);
    }
}

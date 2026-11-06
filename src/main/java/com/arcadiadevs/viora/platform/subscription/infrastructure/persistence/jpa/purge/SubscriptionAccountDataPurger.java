package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.purge;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.DataSharingConsentPersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.PaymentAttemptPersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.QuotaExtensionPersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionGateRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionOperationRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionPersistenceRepository;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Removes the subscriptions of a deleted account with their extensions, payment attempts, idempotent operations and
 * producer gate, and the consents to share their data. A redeemed invitation code stays redeemed: it is not handed out again.
 */
@Component
public class SubscriptionAccountDataPurger {

    private final SubscriptionPersistenceRepository subscriptions;
    private final PaymentAttemptPersistenceRepository attempts;
    private final QuotaExtensionPersistenceRepository extensions;
    private final SubscriptionOperationRepository operations;
    private final SubscriptionGateRepository gates;
    private final DataSharingConsentPersistenceRepository dataSharingConsentPersistenceRepository;

    /**
     * Creates the purger.
     *
     * @param subscriptions                           the subscriptions table
     * @param attempts                                the payment attempts table
     * @param extensions                              the quota extensions table
     * @param operations                              the idempotent operations table
     * @param gates                                   the per producer gates table
     * @param dataSharingConsentPersistenceRepository the data sharing consents table
     */
    public SubscriptionAccountDataPurger(SubscriptionPersistenceRepository subscriptions,
                                         PaymentAttemptPersistenceRepository attempts,
                                         QuotaExtensionPersistenceRepository extensions,
                                         SubscriptionOperationRepository operations,
                                         SubscriptionGateRepository gates,
                                         DataSharingConsentPersistenceRepository dataSharingConsentPersistenceRepository) {
        this.subscriptions = subscriptions;
        this.attempts = attempts;
        this.extensions = extensions;
        this.operations = operations;
        this.gates = gates;
        this.dataSharingConsentPersistenceRepository = dataSharingConsentPersistenceRepository;
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
        dataSharingConsentPersistenceRepository.deleteAll(
                dataSharingConsentPersistenceRepository.findAllByProducerIdOrderByAcceptedAtDesc(
                        UUID.fromString(event.userId())));
        extensions.deleteAll(extensions.findAllByProducerId(UUID.fromString(event.userId())));
        subscriptions.deleteAll(subscriptions.findAllByProducerId(UUID.fromString(event.userId())));
        gates.findById(event.userId()).ifPresent(gates::delete);
    }
}

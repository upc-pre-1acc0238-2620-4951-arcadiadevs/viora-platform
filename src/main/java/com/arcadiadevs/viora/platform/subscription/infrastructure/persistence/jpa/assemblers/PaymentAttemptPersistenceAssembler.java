package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PaymentAttempt;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.PaymentAttemptPersistenceEntity;

/** Unified bidirectional mapping for durable attempts and transaction observations. */
public final class PaymentAttemptPersistenceAssembler {
    private PaymentAttemptPersistenceAssembler() { }

    /**
     * Updates a managed row without copying credentials or dispatching events.
     * @param attempt immutable domain state
     * @param row managed persistence row
     * @return updated row
     */
    public static PaymentAttemptPersistenceEntity toPersistenceFromDomain(PaymentAttempt attempt, PaymentAttemptPersistenceEntity row) {
        row.setId(attempt.id());
        row.setSubscriptionId(attempt.subscriptionId());
        row.setProducerId(attempt.producerId());
        row.setExtensionId(attempt.extensionId());
        row.setStatus(attempt.status());
        row.setPreferenceId(attempt.preferenceId());
        row.setCheckoutUrl(attempt.checkoutUrl());
        row.getPaymentStates().clear();
        row.getPaymentStates().putAll(attempt.paymentStates());
        row.setInitiatedAt(attempt.createdAt());
        row.setExpiresAt(attempt.expiresAt());
        return row;
    }

    /**
     * Rebuilds immutable state from stored transaction observations.
     * @param row stored representation
     * @return immutable attempt
     */
    public static PaymentAttempt toDomainFromPersistence(PaymentAttemptPersistenceEntity row) {
        return new PaymentAttempt(row.getId(), row.getSubscriptionId(), row.getProducerId(), row.getStatus(),
                row.getPreferenceId(), row.getCheckoutUrl(), row.getInitiatedAt(), row.getExpiresAt(), row.getPaymentStates(),
                row.getExtensionId());
    }
}

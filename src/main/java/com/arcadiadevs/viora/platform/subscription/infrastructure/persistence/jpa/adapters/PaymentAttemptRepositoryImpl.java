package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.adapters;

import java.util.*;
import org.springframework.stereotype.Repository;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PaymentAttempt;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.PaymentAttemptRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.PaymentAttemptPersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.PaymentAttemptPersistenceEntity;
/** Explicit conversion isolates JPA rows from the application and domain. */
@Repository
public class PaymentAttemptRepositoryImpl implements PaymentAttemptRepository {
    private final PaymentAttemptPersistenceRepository rows;
    public PaymentAttemptRepositoryImpl(PaymentAttemptPersistenceRepository rows) { this.rows = rows; }
    @Override public PaymentAttempt save(PaymentAttempt attempt) {
        var row = rows.findById(attempt.id()).orElseGet(PaymentAttemptPersistenceEntity::new);
        row.setId(attempt.id()); row.setSubscriptionId(attempt.subscriptionId()); row.setProducerId(attempt.producerId());
        row.setStatus(attempt.status()); row.setPreferenceId(attempt.preferenceId()); row.setCheckoutUrl(attempt.checkoutUrl());
        row.setInitiatedAt(attempt.createdAt()); row.setExpiresAt(attempt.expiresAt()); rows.saveAndFlush(row);
        return attempt;
    }
    @Override public Optional<PaymentAttempt> find(UUID id) { return rows.findById(id).map(this::toDomain); }
    @Override public Optional<PaymentAttempt> latest(UUID id) { return rows.findFirstBySubscriptionIdOrderByInitiatedAtDesc(id).map(this::toDomain); }
    private PaymentAttempt toDomain(PaymentAttemptPersistenceEntity row) {
        return new PaymentAttempt(row.getId(), row.getSubscriptionId(), row.getProducerId(), row.getStatus(),
                row.getPreferenceId(), row.getCheckoutUrl(), row.getInitiatedAt(), row.getExpiresAt());
    }
}

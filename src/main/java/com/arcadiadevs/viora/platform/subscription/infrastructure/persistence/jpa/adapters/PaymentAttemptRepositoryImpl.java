package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.adapters;

import java.util.*;
import org.springframework.stereotype.Repository;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PaymentAttempt;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.PaymentAttemptRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.PaymentAttemptPersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.PaymentAttemptPersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers.PaymentAttemptPersistenceAssembler;
/** Explicit conversion isolates JPA rows from the application and domain. */
@Repository
public class PaymentAttemptRepositoryImpl implements PaymentAttemptRepository {
    private final PaymentAttemptPersistenceRepository rows;
    public PaymentAttemptRepositoryImpl(PaymentAttemptPersistenceRepository rows) { this.rows = rows; }
    @Override public PaymentAttempt save(PaymentAttempt attempt) {
        var row = rows.findById(attempt.id()).orElseGet(PaymentAttemptPersistenceEntity::new);
        rows.saveAndFlush(PaymentAttemptPersistenceAssembler.toPersistenceFromDomain(attempt, row));
        return attempt;
    }
    @Override public Optional<PaymentAttempt> find(UUID id) { return rows.findById(id).map(PaymentAttemptPersistenceAssembler::toDomainFromPersistence); }
    @Override public Optional<PaymentAttempt> latest(UUID id) { return rows.findFirstBySubscriptionIdAndExtensionIdIsNullOrderByInitiatedAtDesc(id).map(PaymentAttemptPersistenceAssembler::toDomainFromPersistence); }
    @Override public Optional<PaymentAttempt> latestForExtension(UUID id) { return rows.findFirstByExtensionIdOrderByInitiatedAtDesc(id).map(PaymentAttemptPersistenceAssembler::toDomainFromPersistence); }
}

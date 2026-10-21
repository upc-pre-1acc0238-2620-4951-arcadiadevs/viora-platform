package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.adapters;

import java.util.*;
import org.springframework.stereotype.Repository;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionPersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionPersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers.SubscriptionPersistenceAssembler;
/** Stores domain snapshots without dispatching events. */
@Repository
public class SubscriptionRepositoryImpl implements SubscriptionRepository {
    private final SubscriptionPersistenceRepository rows;
    public SubscriptionRepositoryImpl(SubscriptionPersistenceRepository rows) { this.rows = rows; }
    @Override public Subscription save(Subscription s) {
        var row = rows.findById(s.snapshot().id()).orElseGet(SubscriptionPersistenceEntity::new);
        rows.saveAndFlush(SubscriptionPersistenceAssembler.toPersistenceFromDomain(s, row));
        return s;
    }
    @Override public Optional<Subscription> findById(UUID id) {
        return rows.findById(id).map(SubscriptionPersistenceAssembler::toDomainFromPersistence);
    }
    @Override public Optional<Subscription> findCurrent(String producerId) {
        return rows.findFirstByProducerIdOrderByRequestedAtDesc(producerId).map(SubscriptionPersistenceAssembler::toDomainFromPersistence);
    }
}

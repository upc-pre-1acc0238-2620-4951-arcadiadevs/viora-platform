package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers.SubscriptionPersistenceAssembler;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;

/**
 * Repository adapter bridging {@link SubscriptionRepository} with Spring Data JPA.
 */
@Repository
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final SubscriptionPersistenceRepository persistenceRepository;

    /**
     * Creates the adapter.
     *
     * @param persistenceRepository the Spring Data repository
     */
    public SubscriptionRepositoryImpl(SubscriptionPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public boolean existsActiveByProducerId(ProducerId producerId, Instant at) {
        return persistenceRepository.existsByProducerIdAndStatusAndStartsAtLessThanEqualAndEndsAtGreaterThan(
                UUID.fromString(producerId.producerId()), SubscriptionStatus.ACTIVE, at, at);
    }

    @Override
    public Subscription save(Subscription subscription) {
        var id = UUID.fromString(subscription.snapshot().id().subscriptionId());
        var entity = persistenceRepository.findById(id)
                .map(existing -> SubscriptionPersistenceAssembler.updateEntityFromDomain(existing, subscription))
                .orElseGet(() -> SubscriptionPersistenceAssembler.toPersistenceFromDomain(subscription));
        return SubscriptionPersistenceAssembler.toDomainFromPersistence(persistenceRepository.saveAndFlush(entity));
    }
    /**
     * Finds a domain contract without exposing a persistence entity.
     * @param id domain identity
     * @return matching contract
     */
    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
        return persistenceRepository.findById(UUID.fromString(id.subscriptionId()))
                .map(SubscriptionPersistenceAssembler::toDomainFromPersistence);
    }

    /**
     * Finds the open individual purchase of a producer.
     * @param producerId producer identity
     * @return the pending contract when present
     */
    @Override
    public Optional<Subscription> findPendingByProducerId(ProducerId producerId) {
        return persistenceRepository.findFirstByProducerIdAndStatusOrderByCreatedAtDesc(
                UUID.fromString(producerId.producerId()), SubscriptionStatus.PENDING_PAYMENT)
                .map(SubscriptionPersistenceAssembler::toDomainFromPersistence);
    }

    /**
     * Prefers existing active access so a pending row cannot hide it.
     * @param producerId owner identity
     * @param at evaluation time
     * @return current domain contract
     */
    @Override
    public Optional<Subscription> findCurrent(ProducerId producerId, Instant at) {
        var owner = UUID.fromString(producerId.producerId());
        var active = persistenceRepository.findFirstByProducerIdAndStatusAndStartsAtLessThanEqualAndEndsAtGreaterThanOrderByStartsAtDesc(
                owner, SubscriptionStatus.ACTIVE, at, at);
        return active.or(() -> persistenceRepository.findRecent(owner, PageRequest.of(0, 1)).stream().findFirst())
                .map(SubscriptionPersistenceAssembler::toDomainFromPersistence);
    }


    /**
     * Restricts ownership in the database before reconstructing the domain contract.
     * @param id requested domain identifier
     * @param producerId authenticated owner
     * @return owned domain contract, or empty when missing or foreign
     */
    @Override
    public Optional<Subscription> findByIdAndProducerId(SubscriptionId id, ProducerId producerId) {
        return persistenceRepository.findByIdAndProducerId(UUID.fromString(id.subscriptionId()),
                UUID.fromString(producerId.producerId()))
                .map(SubscriptionPersistenceAssembler::toDomainFromPersistence);
    }
}

package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SubscriptionPersistenceEntity}.
 */
@Repository
public interface SubscriptionPersistenceRepository extends JpaRepository<SubscriptionPersistenceEntity, UUID> {

    /**
     * Checks whether a producer has a subscription with a status whose window contains an instant.
     *
     * @param producerId the producer identifier
     * @param status     the subscription status
     * @param startsAt   the instant, compared with the start of the window
     * @param endsAt     the same instant, compared with the end of the window
     * @return true if such a subscription exists
     */
    boolean existsByProducerIdAndStatusAndStartsAtLessThanEqualAndEndsAtGreaterThan(
            UUID producerId, SubscriptionStatus status, Instant startsAt, Instant endsAt);
    /**
     * Checks for an open individual purchase.
     * @param producerId owner identity
     * @param status expected state
     * @return whether a matching row exists
     */
    boolean existsByProducerIdAndStatus(UUID producerId, SubscriptionStatus status);

}

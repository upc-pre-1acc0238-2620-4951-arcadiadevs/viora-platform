package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

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

    /**
     * Finds currently effective access independently of newer pending rows.
     * @param producerId owner
     * @param status active state
     * @param start instant compared with the inclusive start
     * @param end instant compared with the exclusive end
     * @return active contract when present
     */
    Optional<SubscriptionPersistenceEntity> findFirstByProducerIdAndStatusAndStartsAtLessThanEqualAndEndsAtGreaterThanOrderByStartsAtDesc(
            UUID producerId, SubscriptionStatus status, Instant start, Instant end);

    /**
     * Reads only the requested page of most recently registered owned contracts.
     * @param producerId owner
     * @param page bounded page request
     * @return recent contracts
     */
    @Query("select s from SubscriptionPersistenceEntity s where s.producerId = :producerId order by s.createdAt desc, s.id desc")
    List<SubscriptionPersistenceEntity> findRecent(UUID producerId, Pageable page);

}

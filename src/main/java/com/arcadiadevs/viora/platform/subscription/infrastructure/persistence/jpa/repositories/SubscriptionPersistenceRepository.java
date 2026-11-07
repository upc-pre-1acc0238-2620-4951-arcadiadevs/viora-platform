package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionPersistenceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
     * Finds the newest contract of an owner in one state.
     * @param producerId owner identity
     * @param status expected state
     * @return matching row when present
     */
    Optional<SubscriptionPersistenceEntity> findFirstByProducerIdAndStatusOrderByCreatedAtDesc(UUID producerId,
            SubscriptionStatus status);

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


    /**
     * Selects a row only when the requested identifier belongs to the authenticated producer.
     * @param id requested contract identifier
     * @param producerId authenticated owner
     * @return owned persistence row
     */
    Optional<SubscriptionPersistenceEntity> findByIdAndProducerId(UUID id, UUID producerId);

    /** Everything of a deleted account, for the account purge. */
    List<SubscriptionPersistenceEntity> findAllByProducerId(UUID producerId);

    /**
     * Lists the producers with an active plan of a license in force at an instant.
     *
     * @param licenseId the license
     * @param status    the active status
     * @param at        the instant
     * @return the producers, without repetitions
     */
    @Query("select distinct subscription.producerId from SubscriptionPersistenceEntity subscription "
            + "where subscription.licenseId = :licenseId and subscription.status = :status "
            + "and subscription.startsAt <= :at and subscription.endsAt > :at")
    List<UUID> findProducerIdsInForceByLicenseId(@Param("licenseId") UUID licenseId,
                                                 @Param("status") SubscriptionStatus status,
                                                 @Param("at") Instant at);

    /**
     * Lists the plans of a license with a status in force at an instant.
     *
     * @param licenseId the license
     * @param status    the status
     * @param at        the instant
     * @return the plan entities
     */
    @Query("select subscription from SubscriptionPersistenceEntity subscription "
            + "where subscription.licenseId = :licenseId and subscription.status = :status "
            + "and subscription.startsAt <= :at and subscription.endsAt > :at")
    List<SubscriptionPersistenceEntity> findInForceByLicenseId(@Param("licenseId") UUID licenseId,
                                                               @Param("status") SubscriptionStatus status,
                                                               @Param("at") Instant at);
}

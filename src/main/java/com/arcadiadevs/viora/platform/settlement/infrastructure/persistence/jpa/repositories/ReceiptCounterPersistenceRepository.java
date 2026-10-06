package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.ReceiptCounterPersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link ReceiptCounterPersistenceEntity}.
 *
 * <p>The two write methods are the same intent expressed in the dialect each database speaks: opening the counter
 * of a producer and campaign that has none. Both are single atomic statements, so a settlement that races another
 * one neither fails nor aborts its transaction, and both leave an already existing counter untouched.</p>
 */
@Repository
public interface ReceiptCounterPersistenceRepository extends JpaRepository<ReceiptCounterPersistenceEntity, UUID> {

    Optional<ReceiptCounterPersistenceEntity> findByProducerIdAndCampaignYear(UUID producerId, Integer campaignYear);

    /** Locks the counter row so two settlements of the same producer and campaign cannot consume the same sequence. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ReceiptCounterPersistenceEntity c where c.producerId = :producerId "
            + "and c.campaignYear = :campaignYear")
    Optional<ReceiptCounterPersistenceEntity> findByProducerIdAndCampaignYearForUpdate(
            @Param("producerId") UUID producerId, @Param("campaignYear") Integer campaignYear);

    /**
     * Postgres spelling: the insert is discarded when the producer and campaign already have a counter.
     *
     * @param id           identifier of the counter to open when none exists
     * @param producerId   producer the counter belongs to
     * @param campaignYear campaign the counter belongs to
     * @return the number of inserted rows, zero when the counter already existed
     */
    @Modifying
    @Query(value = "insert into harvest_receipt_counters (id, producer_id, campaign_year, last_sequence) "
            + "values (:id, :producerId, :campaignYear, 0) "
            + "on conflict (producer_id, campaign_year) do nothing", nativeQuery = true)
    int insertOnConflictDoNothing(@Param("id") UUID id, @Param("producerId") UUID producerId,
            @Param("campaignYear") Integer campaignYear);

    /**
     * H2 spelling of the same intent: H2 has no {@code on conflict} clause, but its {@code merge ... key} form is
     * atomic and re-applies {@code last_sequence} from the row that is already there, so an existing counter keeps
     * the sequences it has already handed out.
     *
     * @param id           identifier of the counter to open when none exists
     * @param producerId   producer the counter belongs to
     * @param campaignYear campaign the counter belongs to
     * @return the number of affected rows, one whether the counter was inserted or left as it was
     */
    @Modifying
    @Query(value = "merge into harvest_receipt_counters (id, producer_id, campaign_year, last_sequence) "
            + "key (producer_id, campaign_year) values (:id, :producerId, :campaignYear, "
            + "coalesce((select max(last_sequence) from harvest_receipt_counters "
            + "where producer_id = :producerId and campaign_year = :campaignYear), 0))", nativeQuery = true)
    int mergeKeepingLastSequence(@Param("id") UUID id, @Param("producerId") UUID producerId,
            @Param("campaignYear") Integer campaignYear);
}

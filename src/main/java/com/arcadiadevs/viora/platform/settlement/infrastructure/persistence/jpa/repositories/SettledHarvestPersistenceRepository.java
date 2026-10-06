package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.HarvestSettlementPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository that reaches a settlement without going through the report of a plot, which is what an
 * idempotent replay needs: the producer may retry the same request from any of their plots.
 *
 * <p>The lookup hits the denormalized {@code producer_id} of the settlement, served by the unique index
 * {@code uq_settlement_producer_idempotency}, and joins the report because the settlement only carries its own
 * columns. Nothing here loads the settlements and filters them in memory.</p>
 */
@Repository
public interface SettledHarvestPersistenceRepository
        extends JpaRepository<HarvestSettlementPersistenceEntity, UUID> {

    /**
     * Finds the settlement a producer registered with an idempotency key.
     *
     * @param producerId     producer that owns the settlement
     * @param idempotencyKey key the settlement was registered with
     * @return the settlement with its report loaded, empty when the producer has none under that key
     */
    @Query("select s from HarvestSettlementPersistenceEntity s join fetch s.report r "
            + "where r.producerId = :producerId and s.idempotencyKey = :idempotencyKey")
    Optional<HarvestSettlementPersistenceEntity> findByProducerIdAndIdempotencyKey(
            @Param("producerId") UUID producerId, @Param("idempotencyKey") String idempotencyKey);
}

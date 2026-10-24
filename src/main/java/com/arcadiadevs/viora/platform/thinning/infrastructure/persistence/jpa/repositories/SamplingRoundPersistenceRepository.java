package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.Collection;

import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities.SamplingRoundPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA persistence repository for {@link SamplingRoundPersistenceEntity}.
 */
@Repository
public interface SamplingRoundPersistenceRepository
        extends JpaRepository<SamplingRoundPersistenceEntity, UUID> {

    /**
     * Checks if a sampling round was already submitted under the given idempotency composite key.
     *
     * @param actorId       the user UUID
     * @param plotId        the plot UUID
     * @param clientBatchId the client batch string
     * @return {@code true} if an entry exists; {@code false} otherwise
     */
    boolean existsByActorIdAndPlotIdAndClientBatchId(UUID actorId, UUID plotId, String clientBatchId);

    /** Everything of a deleted account, for the account purge. */
    List<SamplingRoundPersistenceEntity> findAllByPlotIdIn(Collection<UUID> plotIds);
}

package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.ThinningExecutionRecordPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for the thinning execution projection. */
@Repository
public interface ThinningExecutionRecordPersistenceRepository
        extends JpaRepository<ThinningExecutionRecordPersistenceEntity, UUID> {

    Optional<ThinningExecutionRecordPersistenceEntity> findByPlotIdAndCampaignYear(UUID plotId, Integer campaignYear);

    boolean existsByEventId(String eventId);
}

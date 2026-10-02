package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ThinningExecutionRecord;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.ThinningExecutionRecordRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.ThinningExecutionRecordPersistenceEntity;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.ThinningExecutionRecordPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adapter between the thinning execution projection port and Spring Data JPA. */
@Repository
public class ThinningExecutionRecordRepositoryImpl implements ThinningExecutionRecordRepository {

    private final ThinningExecutionRecordPersistenceRepository persistenceRepository;

    public ThinningExecutionRecordRepositoryImpl(ThinningExecutionRecordPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Optional<ThinningExecutionRecord> findByPlotIdAndCampaignYear(PlotId plotId, Integer campaignYear) {
        return persistenceRepository.findByPlotIdAndCampaignYear(UUID.fromString(plotId.plotId()), campaignYear)
                .map(entity -> new ThinningExecutionRecord(entity.getEventId(), entity.getConfirmationId().toString(),
                        new PlotId(entity.getPlotId().toString()), entity.getCampaignYear(), entity.getExecutedDate(),
                        entity.getPrescribedRemovalPercentage(), entity.getActualRemovalPercentage(),
                        entity.getOnTime()));
    }

    @Override
    public boolean saveIfAbsent(ThinningExecutionRecord record) {
        UUID plotId = UUID.fromString(record.plotId().plotId());
        UUID confirmationId = UUID.fromString(record.confirmationId());
        if (persistenceRepository.existsByEventId(record.eventId())
                || persistenceRepository.existsById(confirmationId)
                || persistenceRepository.findByPlotIdAndCampaignYear(plotId, record.campaignYear()).isPresent()) {
            return false;
        }
        var entity = new ThinningExecutionRecordPersistenceEntity();
        entity.setConfirmationId(confirmationId);
        entity.setEventId(record.eventId());
        entity.setPlotId(plotId);
        entity.setCampaignYear(record.campaignYear());
        entity.setExecutedDate(record.executedDate());
        entity.setPrescribedRemovalPercentage(record.prescribedRemovalPercentage());
        entity.setActualRemovalPercentage(record.actualRemovalPercentage());
        entity.setOnTime(record.onTime());
        persistenceRepository.save(entity);
        return true;
    }
}

package com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.TrackerId;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
import com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.assemblers.ChillAccumulationTrackerPersistenceAssembler;
import com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.entities.ChillAccumulationTrackerPersistenceEntity;
import com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.repositories.ChillAccumulationTrackerPersistenceRepository;
import com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.repositories.HistoricalHarvestEntryPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing {@link ChillAccumulationTrackerRepository} via Spring Data JPA.
 */
@Repository
public class ChillAccumulationTrackerRepositoryImpl implements ChillAccumulationTrackerRepository {

    private final ChillAccumulationTrackerPersistenceRepository trackerPersistenceRepository;
    private final HistoricalHarvestEntryPersistenceRepository harvestEntryPersistenceRepository;

    /**
     * Constructs the repository adapter injecting persistence repositories.
     *
     * @param trackerPersistenceRepository      the tracker JPA repository
     * @param harvestEntryPersistenceRepository the harvest entries JPA repository
     */
    public ChillAccumulationTrackerRepositoryImpl(
            ChillAccumulationTrackerPersistenceRepository trackerPersistenceRepository,
            HistoricalHarvestEntryPersistenceRepository harvestEntryPersistenceRepository
    ) {
        this.trackerPersistenceRepository = trackerPersistenceRepository;
        this.harvestEntryPersistenceRepository = harvestEntryPersistenceRepository;
    }

    @Override
    public ChillAccumulationTracker save(ChillAccumulationTracker tracker) {
        var uuid = UUID.fromString(tracker.snapshot().id().trackerId());
        var plotUuid = UUID.fromString(tracker.snapshot().plotId().plotId());
        var existingOpt = trackerPersistenceRepository.findById(uuid)
                .or(() -> trackerPersistenceRepository.findByPlotId(plotUuid));

        ChillAccumulationTrackerPersistenceEntity entityToSave;
        if (existingOpt.isPresent()) {
            entityToSave = ChillAccumulationTrackerPersistenceAssembler.updateEntityFromDomain(existingOpt.get(), tracker);
        } else {
            entityToSave = ChillAccumulationTrackerPersistenceAssembler.toPersistenceFromDomain(tracker);
        }

        var saved = trackerPersistenceRepository.save(entityToSave);
        return ChillAccumulationTrackerPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<ChillAccumulationTracker> findById(TrackerId id) {
        return trackerPersistenceRepository.findById(UUID.fromString(id.trackerId()))
                .map(ChillAccumulationTrackerPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<ChillAccumulationTracker> findByPlotId(PlotId plotId) {
        return trackerPersistenceRepository.findByPlotId(UUID.fromString(plotId.plotId()))
                .map(ChillAccumulationTrackerPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByPlotIdAndCampaignYear(PlotId plotId, CampaignYear year) {
        return harvestEntryPersistenceRepository.existsByPlotIdAndCampaignYear(
                UUID.fromString(plotId.plotId()),
                year.value()
        );
    }
}

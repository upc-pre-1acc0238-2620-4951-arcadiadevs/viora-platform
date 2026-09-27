package com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTrackerSnapshot;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.HistoricalHarvestEntrySnapshot;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.entities.ChillAccumulationTrackerPersistenceEntity;
import com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.entities.HistoricalHarvestEntryPersistenceEntity;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Unified persistence assembler bridging domain {@link ChillAccumulationTracker} aggregate roots
 * and JPA {@link ChillAccumulationTrackerPersistenceEntity} representations.
 */
public final class ChillAccumulationTrackerPersistenceAssembler {

    private ChillAccumulationTrackerPersistenceAssembler() {
    }

    /**
     * Converts a domain aggregate to a new JPA persistence entity.
     *
     * @param domain the domain aggregate
     * @return the mapped JPA entity
     */
    public static ChillAccumulationTrackerPersistenceEntity toPersistenceFromDomain(ChillAccumulationTracker domain) {
        if (domain == null) {
            throw new IllegalArgumentException("phenology.tracker.snapshot.null");
        }
        var snap = domain.snapshot();
        var entity = new ChillAccumulationTrackerPersistenceEntity();
        entity.setId(UUID.fromString(snap.id().trackerId()));
        entity.setPlotId(UUID.fromString(snap.plotId().plotId()));
        entity.setCampaignYear(snap.currentCampaign().value());
        entity.setErezPortions(0.0);
        entity.setCalculatedBbi(snap.calculatedBbi().value());

        var childEntities = new ArrayList<HistoricalHarvestEntryPersistenceEntity>();
        for (var entrySnap : snap.harvestHistory()) {
            var child = new HistoricalHarvestEntryPersistenceEntity();
            child.setId(UUID.fromString(entrySnap.id().harvestEntryId()));
            child.setTracker(entity);
            child.setPlotId(UUID.fromString(snap.plotId().plotId()));
            child.setCampaignYear(entrySnap.campaignYear().value());
            child.setTotalYieldKg(entrySnap.harvestYield().totalKg());
            child.setGreenKg(entrySnap.harvestYield().greenKg());
            child.setBlackKg(entrySnap.harvestYield().blackKg());
            child.setBearingClassification(entrySnap.classification().name());
            child.setRecordedAt(entrySnap.recordedAt());
            childEntities.add(child);
        }
        entity.setHarvestRecords(childEntities);
        // Revision is intentionally left null for transient entities so Hibernate executes an INSERT
        // and initializes the @Version counter properly without marking transactions rollback-only.

        return entity;
    }

    /**
     * Synchronizes state from the domain aggregate into an existing managed JPA entity.
     *
     * @param target the managed JPA entity
     * @param domain the domain aggregate with updated state
     * @return the updated target entity
     */
    public static ChillAccumulationTrackerPersistenceEntity updateEntityFromDomain(
            ChillAccumulationTrackerPersistenceEntity target,
            ChillAccumulationTracker domain
    ) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("phenology.tracker.snapshot.null");
        }
        var snap = domain.snapshot();
        target.setCampaignYear(snap.currentCampaign().value());
        target.setCalculatedBbi(snap.calculatedBbi().value());

        // Sync children
        var existingMap = new java.util.HashMap<UUID, HistoricalHarvestEntryPersistenceEntity>();
        for (var child : target.getHarvestRecords()) {
            existingMap.put(child.getId(), child);
        }

        var updatedChildren = new ArrayList<HistoricalHarvestEntryPersistenceEntity>();
        for (var entrySnap : snap.harvestHistory()) {
            var entryUuid = UUID.fromString(entrySnap.id().harvestEntryId());
            var child = existingMap.getOrDefault(entryUuid, new HistoricalHarvestEntryPersistenceEntity());
            child.setId(entryUuid);
            child.setTracker(target);
            child.setPlotId(UUID.fromString(snap.plotId().plotId()));
            child.setCampaignYear(entrySnap.campaignYear().value());
            child.setTotalYieldKg(entrySnap.harvestYield().totalKg());
            child.setGreenKg(entrySnap.harvestYield().greenKg());
            child.setBlackKg(entrySnap.harvestYield().blackKg());
            child.setBearingClassification(entrySnap.classification().name());
            child.setRecordedAt(entrySnap.recordedAt());
            updatedChildren.add(child);
        }

        target.getHarvestRecords().clear();
        target.getHarvestRecords().addAll(updatedChildren);

        return target;
    }

    /**
     * Converts a JPA entity into a reconstituted domain aggregate root.
     *
     * @param entity the JPA entity
     * @return the reconstituted {@link ChillAccumulationTracker}
     */
    public static ChillAccumulationTracker toDomainFromPersistence(ChillAccumulationTrackerPersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("phenology.tracker.snapshot.null");
        }

        var entrySnapshots = new ArrayList<HistoricalHarvestEntrySnapshot>();
        if (entity.getHarvestRecords() != null) {
            for (var child : entity.getHarvestRecords()) {
                var entrySnap = new HistoricalHarvestEntrySnapshot(
                        new HarvestEntryId(child.getId().toString()),
                        new CampaignYear(child.getCampaignYear()),
                        new HarvestYield(child.getTotalYieldKg(), child.getGreenKg(), child.getBlackKg()),
                        BearingClassification.valueOf(child.getBearingClassification()),
                        child.getRecordedAt()
                );
                entrySnapshots.add(entrySnap);
            }
        }

        var snapshot = new ChillAccumulationTrackerSnapshot(
                new TrackerId(entity.getId().toString()),
                new PlotId(entity.getPlotId().toString()),
                new CampaignYear(entity.getCampaignYear()),
                entrySnapshots,
                new BiennialBearingIndex(entity.getCalculatedBbi() != null ? entity.getCalculatedBbi() : 0.0),
                entity.getRevision()
        );

        return ChillAccumulationTracker.reconstitute(snapshot);
    }
}

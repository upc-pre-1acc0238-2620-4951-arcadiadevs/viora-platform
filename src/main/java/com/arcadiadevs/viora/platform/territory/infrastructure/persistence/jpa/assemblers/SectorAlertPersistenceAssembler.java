package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorAlert;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorAlertSnapshot;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertFacts;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.SectorAlertPersistenceEntity;

import java.util.UUID;

/**
 * Assembler between {@link SectorAlert} and {@link SectorAlertPersistenceEntity}.
 */
public final class SectorAlertPersistenceAssembler {

    private SectorAlertPersistenceAssembler() {
    }

    /**
     * Copies the state of an alert onto an entity, new or managed, leaving the revision to JPA.
     *
     * @param target the entity to update
     * @param domain the alert
     * @return the updated entity
     * @throws IllegalArgumentException if an argument is null
     */
    public static SectorAlertPersistenceEntity toPersistenceFromDomain(SectorAlertPersistenceEntity target,
                                                                       SectorAlert domain) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("territory.sector_alert.entity_or_domain.null");
        }
        var snapshot = domain.snapshot();
        var facts = snapshot.facts();
        target.setId(UUID.fromString(snapshot.id().sectorAlertId()));
        target.setLicenseId(UUID.fromString(snapshot.licenseId().licenseId()));
        target.setSectorZoneId(UUID.fromString(snapshot.sectorZoneId().sectorZoneId()));
        target.setType(snapshot.type());
        target.setAffectedPlots(facts.affectedPlots());
        target.setSectorPlots(facts.sectorPlots());
        target.setLeadPlotId(facts.leadPlotId() == null ? null : UUID.fromString(facts.leadPlotId().plotId()));
        target.setLeadProducerId(facts.leadProducerId() == null ? null
                : UUID.fromString(facts.leadProducerId().producerId()));
        target.setLeadReason(facts.leadReason());
        target.setLeadValue(facts.leadValue());
        target.setLeadDate(facts.leadDate());
        target.setOpenedAt(snapshot.openedAt());
        target.setFactsUpdatedAt(snapshot.updatedAt());
        target.setNormalizedAt(snapshot.normalizedAt());
        target.setLevelAtNormalize(snapshot.levelAtNormalize());
        return target;
    }

    /**
     * Rebuilds an alert from its entity.
     *
     * @param entity the entity
     * @return the alert
     * @throws IllegalArgumentException if the entity is null
     */
    public static SectorAlert toDomainFromPersistence(SectorAlertPersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("territory.sector_alert.entity.null");
        }
        var facts = new SectorAlertFacts(entity.getAffectedPlots(), entity.getSectorPlots(),
                entity.getLeadPlotId() == null ? null : new PlotId(entity.getLeadPlotId().toString()),
                entity.getLeadProducerId() == null ? null : new ProducerId(entity.getLeadProducerId().toString()),
                entity.getLeadReason(), entity.getLeadValue(), entity.getLeadDate());
        return SectorAlert.reconstitute(new SectorAlertSnapshot(new SectorAlertId(entity.getId().toString()),
                new LicenseId(entity.getLicenseId().toString()), new SectorZoneId(entity.getSectorZoneId().toString()),
                entity.getType(), facts, entity.getOpenedAt(), entity.getFactsUpdatedAt(), entity.getNormalizedAt(),
                entity.getLevelAtNormalize(), entity.getRevision()));
    }
}

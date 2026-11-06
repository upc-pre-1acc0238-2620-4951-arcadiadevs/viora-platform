package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignment;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignmentSnapshot;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorAssignmentId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.PlotSectorAssignmentPersistenceEntity;

import java.util.UUID;

/**
 * Assembler between {@link PlotSectorAssignment} and {@link PlotSectorAssignmentPersistenceEntity}.
 */
public final class PlotSectorAssignmentPersistenceAssembler {

    private PlotSectorAssignmentPersistenceAssembler() {
    }

    /**
     * Copies the state of an assignment onto an entity, new or managed, leaving the revision to JPA.
     *
     * @param target the entity to update
     * @param domain the assignment
     * @return the updated entity
     * @throws IllegalArgumentException if an argument is null
     */
    public static PlotSectorAssignmentPersistenceEntity toPersistenceFromDomain(
            PlotSectorAssignmentPersistenceEntity target, PlotSectorAssignment domain) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("territory.plot_sector.entity_or_domain.null");
        }
        var snapshot = domain.snapshot();
        target.setId(UUID.fromString(snapshot.id().plotSectorAssignmentId()));
        target.setLicenseId(UUID.fromString(snapshot.licenseId().licenseId()));
        target.setPlotId(UUID.fromString(snapshot.plotId().plotId()));
        target.setProducerId(UUID.fromString(snapshot.producerId().producerId()));
        target.setStatus(snapshot.status());
        target.setSectorZoneId(snapshot.sectorZoneId() == null ? null
                : UUID.fromString(snapshot.sectorZoneId().sectorZoneId()));
        target.setProposedName(snapshot.proposedName() == null ? null : snapshot.proposedName().value());
        target.setNormalizedProposedName(snapshot.proposedName() == null ? null
                : snapshot.proposedName().normalized());
        target.setSectorUpdatedAt(snapshot.updatedAt());
        return target;
    }

    /**
     * Rebuilds an assignment from its entity.
     *
     * @param entity the entity
     * @return the assignment
     * @throws IllegalArgumentException if the entity is null
     */
    public static PlotSectorAssignment toDomainFromPersistence(PlotSectorAssignmentPersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("territory.plot_sector.entity.null");
        }
        return PlotSectorAssignment.reconstitute(new PlotSectorAssignmentSnapshot(
                new PlotSectorAssignmentId(entity.getId().toString()),
                new LicenseId(entity.getLicenseId().toString()),
                new PlotId(entity.getPlotId().toString()),
                new ProducerId(entity.getProducerId().toString()),
                entity.getStatus(),
                entity.getSectorZoneId() == null ? null : new SectorZoneId(entity.getSectorZoneId().toString()),
                entity.getProposedName() == null ? null : new SectorZoneName(entity.getProposedName()),
                entity.getSectorUpdatedAt(),
                entity.getRevision()));
    }
}

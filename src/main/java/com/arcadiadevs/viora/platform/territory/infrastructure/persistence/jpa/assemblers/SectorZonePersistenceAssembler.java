package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZone;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZoneSnapshot;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.SectorZonePersistenceEntity;

import java.util.UUID;

/**
 * Assembler between {@link SectorZone} and {@link SectorZonePersistenceEntity}.
 */
public final class SectorZonePersistenceAssembler {

    private SectorZonePersistenceAssembler() {
    }

    /**
     * Copies the state of a zone onto an entity, new or managed, leaving the revision to JPA.
     *
     * @param target the entity to update
     * @param domain the zone
     * @return the updated entity
     * @throws IllegalArgumentException if an argument is null
     */
    public static SectorZonePersistenceEntity toPersistenceFromDomain(SectorZonePersistenceEntity target,
                                                                      SectorZone domain) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("territory.sector_zone.entity_or_domain.null");
        }
        var snapshot = domain.snapshot();
        target.setId(UUID.fromString(snapshot.id().sectorZoneId()));
        target.setLicenseId(UUID.fromString(snapshot.licenseId().licenseId()));
        target.setName(snapshot.name().value());
        target.setNormalizedName(snapshot.name().normalized());
        return target;
    }

    /**
     * Rebuilds a zone from its entity.
     *
     * @param entity the entity
     * @return the zone
     * @throws IllegalArgumentException if the entity is null
     */
    public static SectorZone toDomainFromPersistence(SectorZonePersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("territory.sector_zone.entity.null");
        }
        return SectorZone.reconstitute(new SectorZoneSnapshot(new SectorZoneId(entity.getId().toString()),
                new LicenseId(entity.getLicenseId().toString()), new SectorZoneName(entity.getName()),
                entity.getRevision()));
    }
}

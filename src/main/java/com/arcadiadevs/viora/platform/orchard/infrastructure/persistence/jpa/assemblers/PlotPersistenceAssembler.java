package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.PlotSnapshot;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.embeddables.PlantationFramePersistenceEmbeddable;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.embeddables.PlotGeometryPersistenceEmbeddable;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.entities.PlotPersistenceEntity;

import java.util.UUID;

/**
 * Unified persistence assembler translating between the domain {@link Plot} aggregate
 * and the JPA {@link PlotPersistenceEntity}.
 */
public final class PlotPersistenceAssembler {

    private PlotPersistenceAssembler() {
    }

    public static PlotPersistenceEntity toPersistenceFromDomain(Plot domain) {
        return toEntity(domain);
    }

    /**
     * Maps a domain {@link Plot} aggregate into a JPA {@link PlotPersistenceEntity}.
     *
     * @param domain the domain aggregate to map
     * @return the corresponding JPA persistence entity
     */
    public static PlotPersistenceEntity toEntity(Plot domain) {
        if (domain == null) {
            throw new IllegalArgumentException("plot.domain.null");
        }
        var snap = domain.snapshot();

        var entity = new PlotPersistenceEntity();
        entity.setId(UUID.fromString(snap.id().plotId()));
        entity.setProducerId(snap.producerId());
        entity.setName(snap.name());
        entity.setVariety(snap.variety());
        entity.setGeometry(new PlotGeometryPersistenceEmbeddable(
                snap.geometry().geoJson(),
                snap.geometry().areaHa()
        ));
        entity.setFrame(new PlantationFramePersistenceEmbeddable(
                snap.frame().rowSpacingM(),
                snap.frame().treeSpacingM()
        ));
        entity.setDensity(snap.density());
        entity.setLastPruningDate(snap.lastPruningDate());
        entity.setStatus(snap.status());
        if (snap.revision() != null && snap.revision() > 0L) {
            entity.setRevision(snap.revision());
        }

        return entity;
    }

    /**
     * Updates an existing managed JPA entity state from the domain aggregate snapshot,
     * allowing JPA / Hibernate to handle @Version revision incrementing naturally.
     *
     * @param target the managed JPA entity to update
     * @param domain the domain aggregate containing updated state
     * @return the updated JPA entity
     */
    public static PlotPersistenceEntity updateEntityFromDomain(PlotPersistenceEntity target, Plot domain) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("plot.entity_or_domain.null");
        }
        var snap = domain.snapshot();
        target.setName(snap.name());
        target.setVariety(snap.variety());
        target.setGeometry(new PlotGeometryPersistenceEmbeddable(
                snap.geometry().geoJson(),
                snap.geometry().areaHa()
        ));
        target.setFrame(new PlantationFramePersistenceEmbeddable(
                snap.frame().rowSpacingM(),
                snap.frame().treeSpacingM()
        ));
        target.setDensity(snap.density());
        target.setLastPruningDate(snap.lastPruningDate());
        target.setStatus(snap.status());
        return target;
    }

    /**
     * Maps a JPA {@link PlotPersistenceEntity} into a domain {@link Plot} aggregate.
     *
     * @param entity the JPA entity to map
     * @return the reconstituted domain Plot aggregate
     */
    public static Plot toDomainFromPersistence(PlotPersistenceEntity entity) {
        return toDomain(entity);
    }

    /**
     * Maps a JPA {@link PlotPersistenceEntity} into a domain {@link Plot} aggregate.
     *
     * @param entity the JPA entity to map
     * @return the reconstituted domain Plot aggregate
     */
    public static Plot toDomain(PlotPersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("plot.entity.null");
        }

        var geometryVo = entity.getGeometry() != null
                ? new PlotGeometry(entity.getGeometry().getPolygonGeoJson(), entity.getGeometry().getAreaHa())
                : null;
        var frameVo = entity.getFrame() != null
                ? new PlantationFrame(entity.getFrame().getRowSpacingM(), entity.getFrame().getTreeSpacingM())
                : null;

        var snapshot = new PlotSnapshot(
                new PlotId(entity.getId().toString()),
                entity.getProducerId(),
                entity.getName(),
                entity.getVariety(),
                geometryVo,
                frameVo,
                entity.getDensity(),
                entity.getLastPruningDate(),
                entity.getStatus(),
                entity.getRevision()
        );

        return Plot.reconstitute(snapshot);
    }
}

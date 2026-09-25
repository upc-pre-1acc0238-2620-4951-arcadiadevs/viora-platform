package com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.PlotResource;

import java.util.List;

/**
 * Assembler to convert a Plot entity or list of entities to PlotResource representations.
 */
public class PlotResourceFromEntityAssembler {

    /**
     * Converts a Plot entity to a PlotResource.
     *
     * @param entity The {@link Plot} entity to convert.
     * @return The {@link PlotResource} resource that results from the conversion.
     */
    public static PlotResource toResourceFromEntity(Plot entity) {
        var snap = entity.snapshot();
        return new PlotResource(
                snap.id().plotId(),
                snap.producerId().producerId(),
                snap.name().value(),
                snap.variety().name(),
                snap.geometry().areaHa(),
                snap.density().treesPerHectare(),
                snap.frame().rowSpacingM(),
                snap.frame().treeSpacingM(),
                snap.geometry().geoJson(),
                snap.lastPruningDate(),
                snap.status().name(),
                snap.revision()
        );
    }

    /**
     * Converts a list of Plot entities to a list of PlotResources.
     *
     * @param entities The list of {@link Plot} entities to convert.
     * @return The list of {@link PlotResource} resources.
     */
    public static List<PlotResource> toResourceList(List<Plot> entities) {
        return entities.stream()
                .map(PlotResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
}

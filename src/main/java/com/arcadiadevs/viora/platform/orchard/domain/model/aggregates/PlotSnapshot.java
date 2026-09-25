package com.arcadiadevs.viora.platform.orchard.domain.model.aggregates;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.*;

import java.time.LocalDate;

/**
 * Immutable snapshot representing the state of a Plot aggregate.
 * Used for persistence and presentation without breaking domain encapsulation.
 *
 * @param id              the unique plot identity
 * @param producerId      the producer identifier
 * @param name            the plot name
 * @param variety         the botanical olive variety
 * @param geometry        the cadastral geometry and area
 * @param frame           the planting frame grid
 * @param density         the calculated tree density
 * @param lastPruningDate optional date of last pruning
 * @param status          the lifecycle status
 * @param revision        the optimistic locking revision
 */
public record PlotSnapshot(
        PlotId id,
        ProducerId producerId,
        PlotName name,
        OliveVariety variety,
        PlotGeometry geometry,
        PlantationFrame frame,
        TreeDensity density,
        LocalDate lastPruningDate,
        PlotStatus status,
        Long revision
) {
}

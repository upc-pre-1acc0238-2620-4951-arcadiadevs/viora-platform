package com.arcadiadevs.viora.platform.orchard.domain.model.events;

import java.time.Instant;

/**
 * Domain event dispatched when an existing plot is integrally updated with new boundaries,
 * agronomic parameters, dendrometric density, or denomination under optimistic concurrency control.
 *
 * @param plotId         unique identifier of the updated plot
 * @param producerId     owner of the plot
 * @param name           new plot denomination
 * @param polygonGeoJson GeoJSON string defining the updated boundaries
 * @param areaHa         recalculated net cadastral area in hectares
 * @param revision       new plot revision number following optimistic lock increment
 * @param occurredOn     timestamp when the domain event occurred
 */
public record PlotUpdated(
        String plotId,
        String producerId,
        String name,
        String polygonGeoJson,
        Double areaHa,
        Long revision,
        Instant occurredOn
) {

    /**
     * Compact constructor validating event invariants and non-null values.
     */
    public PlotUpdated {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.event.plot_id.null");
        }
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("plot.event.producer_id.null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("plot.event.name.null");
        }
        if (polygonGeoJson == null || polygonGeoJson.isBlank()) {
            throw new IllegalArgumentException("plot.event.polygon.null");
        }
        if (areaHa == null || areaHa <= 0.0) {
            throw new IllegalArgumentException("plot.event.area.invalid");
        }
        if (revision == null || revision < 0L) {
            throw new IllegalArgumentException("plot.event.revision.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("plot.event.occurred_on.null");
        }
    }
}

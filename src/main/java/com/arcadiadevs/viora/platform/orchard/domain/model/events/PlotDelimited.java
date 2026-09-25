package com.arcadiadevs.viora.platform.orchard.domain.model.events;

import java.time.Instant;

/**
 * Domain event dispatched when a new olive orchard plot has been delimited and registered.
 *
 * @param plotId         the unique identifier string of the plot
 * @param producerId     the owner or manager producer identifier string
 * @param polygonGeoJson the cadastral polygon in GeoJSON format
 * @param variety        the botanical variety name
 * @param occurredOn     the exact UTC timestamp when the event occurred
 */
public record PlotDelimited(
        String plotId,
        String producerId,
        String polygonGeoJson,
        String variety,
        Instant occurredOn
) {

    /**
     * Compact constructor enforcing non-null event fields.
     */
    public PlotDelimited {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.event.plot_id.null");
        }
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("plot.event.producer_id.null");
        }
        if (polygonGeoJson == null || polygonGeoJson.isBlank()) {
            throw new IllegalArgumentException("plot.event.polygon.null");
        }
        if (variety == null || variety.isBlank()) {
            throw new IllegalArgumentException("plot.event.variety.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("plot.event.occurred_on.null");
        }
    }
}

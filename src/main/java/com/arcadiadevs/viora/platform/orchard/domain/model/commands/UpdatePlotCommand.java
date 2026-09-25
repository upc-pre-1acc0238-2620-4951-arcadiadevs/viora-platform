package com.arcadiadevs.viora.platform.orchard.domain.model.commands;

import java.time.LocalDate;

/**
 * Command to update the agronomic boundaries and dendrometric frame of an orchard plot.
 *
 * @param plotId           the target plot identifier
 * @param producerId       the managing producer identifier
 * @param name             the updated plot name
 * @param rowSpacingM      the updated distance between rows in meters
 * @param treeSpacingM     the updated distance between trees in meters
 * @param lastPruningDate  the updated date of last pruning (nullable)
 * @param polygonGeoJson   the updated cadastral polygon GeoJSON string
 * @param expectedRevision the expected optimistic concurrency revision from If-Match
 */
public record UpdatePlotCommand(
        String plotId,
        String producerId,
        String name,
        Double rowSpacingM,
        Double treeSpacingM,
        LocalDate lastPruningDate,
        String polygonGeoJson,
        long expectedRevision
) {

    /**
     * Compact constructor enforcing non-null constraints and valid expected revision.
     */
    public UpdatePlotCommand {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("plot.name.blank");
        }
        if (polygonGeoJson == null || polygonGeoJson.isBlank()) {
            throw new IllegalArgumentException("plot.geometry.empty");
        }
        if (rowSpacingM == null || treeSpacingM == null) {
            throw new IllegalArgumentException("plot.spacing.positive");
        }
        if (expectedRevision < 0L) {
            throw new IllegalArgumentException("plot.revision.invalid");
        }
    }
}

package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing a logical reference to an orchard plot monitored for phenological metrics.
 * Decouples cross-context operations without physical foreign key constraints.
 *
 * @param plotId the UUID string representation
 */
public record PlotId(String plotId) {

    /**
     * Compact constructor validating that the plotId is a non-blank, valid UUID string.
     *
     * @throws IllegalArgumentException if the plotId is null, blank, or malformed
     */
    public PlotId {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        try {
            plotId = UUID.fromString(plotId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("plot.id.invalid_uuid", exception);
        }
    }
}

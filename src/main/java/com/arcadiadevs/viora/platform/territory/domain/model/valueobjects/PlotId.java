package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.util.UUID;

/**
 * Logical reference to a plot, kept by the Orchard context.
 *
 * @param plotId the identifier as a lowercase UUID string
 */
public record PlotId(String plotId) {

    /**
     * Generates a new random identifier.
     */
    public PlotId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public PlotId {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("territory.plot.id.null_or_empty");
        }
        try {
            plotId = UUID.fromString(plotId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("territory.plot.id.invalid_uuid", exception);
        }
    }
}

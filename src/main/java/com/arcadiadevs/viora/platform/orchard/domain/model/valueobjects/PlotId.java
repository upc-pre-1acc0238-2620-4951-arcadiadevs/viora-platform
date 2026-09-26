package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the plot identifier.
 *
 * <p>
 * This value object is used to represent the plot identifier.
 * The identifier is stored as a UUID string to avoid unnecessary coupling.
 * It throws an IllegalArgumentException if the plot id is null, empty, or not a valid UUID.
 * </p>
 *
 * @param plotId The plot id. It must be a valid UUID string.
 */
public record PlotId(String plotId) {

    /**
     * Default constructor.
     * Generates a new random UUID and sets it as the plot id string.
     */
    public PlotId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor for PlotId.
     * Validates that the plotId is a valid UUID string.
     *
     * @throws IllegalArgumentException if the plotId is null, blank, or not a valid UUID.
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

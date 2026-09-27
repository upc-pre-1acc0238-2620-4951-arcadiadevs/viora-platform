package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the logical cross-context reference to a plot in the Orchard Bounded Context.
 * Ensures cross-context decoupling without direct database foreign keys or domain entity sharing.
 *
 * @param plotId the unique plot identifier UUID string
 */
public record PlotId(String plotId) {

    /**
     * Default constructor generating a new random UUID.
     */
    public PlotId() {
        this(UUID.randomUUID().toString());
    }

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

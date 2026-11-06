package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identity of the sector given to a plot within a license.
 *
 * @param plotSectorAssignmentId the identifier as a lowercase UUID string
 */
public record PlotSectorAssignmentId(String plotSectorAssignmentId) {

    /**
     * Generates a new random identifier.
     */
    public PlotSectorAssignmentId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public PlotSectorAssignmentId {
        if (plotSectorAssignmentId == null || plotSectorAssignmentId.isBlank()) {
            throw new IllegalArgumentException("territory.plot_sector.id.null_or_empty");
        }
        try {
            plotSectorAssignmentId = UUID.fromString(plotSectorAssignmentId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("territory.plot_sector.id.invalid_uuid", exception);
        }
    }
}

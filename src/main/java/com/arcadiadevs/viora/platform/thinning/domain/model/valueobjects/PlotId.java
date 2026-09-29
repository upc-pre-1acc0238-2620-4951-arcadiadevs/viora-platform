package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value Object referencing an external Plot aggregate in Orchard context.
 *
 * @param plotId the string UUID value
 */
public record PlotId(String plotId) {

    /**
     * Compact constructor validating that plotId is a non-null valid UUID.
     */
    public PlotId {
        if (plotId == null || plotId.trim().isEmpty()) {
            throw new IllegalArgumentException("thinning.plot.id.null_or_empty");
        }
        try {
            plotId = UUID.fromString(plotId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("thinning.plot.id.invalid_uuid", e);
        }
    }
}

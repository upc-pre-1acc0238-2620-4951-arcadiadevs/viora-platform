package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value Object referencing an external Plot aggregate in Orchard context.
 *
 * @param plotId the string UUID value
 */
public record PlotId(String plotId) {

    /**
     * Compact constructor validating that plotId is a valid UUID.
     */
    public PlotId {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("assistant.plot.invalid");
        }
        try {
            plotId = UUID.fromString(plotId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("assistant.plot.invalid", e);
        }
    }
}

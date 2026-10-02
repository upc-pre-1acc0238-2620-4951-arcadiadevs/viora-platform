package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.util.UUID;

/**
 * Settlement's own reference to an Orchard plot.
 *
 * @param plotId UUID string
 */
public record PlotId(String plotId) {

    public PlotId {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("settlement.plot.id.null_or_empty");
        }
        try {
            plotId = UUID.fromString(plotId.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("settlement.plot.id.invalid_uuid", e);
        }
    }
}

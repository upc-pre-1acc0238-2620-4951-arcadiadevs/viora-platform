package com.arcadiadevs.viora.platform.phenology.domain.model.events;

import java.time.Instant;

/**
 * Domain event dispatched when the Biennial Bearing Index (Hoblyn BBI) is updated following a harvest entry change.
 *
 * @param plotId         the identifier of the monitored plot
 * @param bbiValue       the recalculated Hoblyn BBI value
 * @param classification the bearing classification string
 * @param occurredOn     the exact timestamp when the assessment occurred
 */
public record BiennialBearingIndexAssessedEvent(
        String plotId,
        Double bbiValue,
        String classification,
        Instant occurredOn
) {

    /**
     * Compact constructor enforcing non-null event invariants.
     *
     * @throws IllegalArgumentException if mandatory fields are null or blank
     */
    public BiennialBearingIndexAssessedEvent {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("phenology.event.plot_id.null");
        }
        if (bbiValue == null) {
            throw new IllegalArgumentException("phenology.bbi.invalid_range");
        }
        if (classification == null || classification.isBlank()) {
            throw new IllegalArgumentException("phenology.command.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("phenology.event.occurred_on.null");
        }
    }
}

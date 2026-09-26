package com.arcadiadevs.viora.platform.orchard.domain.model.events;

import java.time.Instant;

/**
 * Domain event dispatched when an olive orchard plot has been deactivated or soft-deleted from inventory.
 *
 * @param plotId     the unique identifier string of the plot
 * @param producerId the owner or managing producer identifier string
 * @param reason     the justification or reason for removal
 * @param occurredOn the exact UTC timestamp when the event occurred
 */
public record PlotRemovedEvent(
        String plotId,
        String producerId,
        String reason,
        Instant occurredOn
) {

    /**
     * Compact constructor enforcing non-null event fields.
     */
    public PlotRemovedEvent {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.event.plot_id.null");
        }
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("plot.event.producer_id.null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("plot.event.reason.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("plot.event.occurred_on.null");
        }
    }
}

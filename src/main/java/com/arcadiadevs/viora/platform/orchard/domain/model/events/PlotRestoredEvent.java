package com.arcadiadevs.viora.platform.orchard.domain.model.events;

import java.time.Instant;

/**
 * Domain event emitted when an archived orchard plot returns to the active inventory.
 *
 * @param plotId     the restored plot identifier
 * @param producerId the managing producer identifier
 * @param revision   the plot revision after the restoration
 * @param occurredOn the instant of the restoration
 */
public record PlotRestoredEvent(
        String plotId,
        String producerId,
        Long revision,
        Instant occurredOn
) {

    /**
     * Canonical constructor validating the structural invariants of the event.
     */
    public PlotRestoredEvent {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.event.plot_id.null");
        }
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("plot.event.producer_id.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("plot.event.occurred_on.null");
        }
    }
}

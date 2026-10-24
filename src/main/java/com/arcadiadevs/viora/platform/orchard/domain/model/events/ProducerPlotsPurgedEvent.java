package com.arcadiadevs.viora.platform.orchard.domain.model.events;

import java.time.Instant;
import java.util.List;

/**
 * Domain event published when the plots of a deleted account are about to be removed for good.
 *
 * <p>Every context that keeps data per plot (sampling, prescriptions, harvests, chill, telemetry, settlements)
 * reacts to it in the same transaction by removing that data.</p>
 *
 * @param producerId the deleted producer identifier string
 * @param plotIds    every plot of the producer, active or archived
 * @param occurredOn the exact UTC timestamp when the event occurred
 */
public record ProducerPlotsPurgedEvent(
        String producerId,
        List<String> plotIds,
        Instant occurredOn
) {

    /**
     * Compact constructor enforcing non-null event fields.
     */
    public ProducerPlotsPurgedEvent {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("plot.event.producer_id.null");
        }
        if (plotIds == null) {
            throw new IllegalArgumentException("plot.event.plot_ids.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("plot.event.occurred_on.null");
        }
        plotIds = List.copyOf(plotIds);
    }
}

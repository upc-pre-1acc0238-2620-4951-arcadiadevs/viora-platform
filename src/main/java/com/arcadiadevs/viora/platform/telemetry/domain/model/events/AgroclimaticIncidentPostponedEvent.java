package com.arcadiadevs.viora.platform.telemetry.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when an agroclimatic incident is postponed (snoozed) by a producer.
 *
 * @param incidentId   the incident UUID string
 * @param plotId       the plot UUID string
 * @param snoozedUntil the instant until which the incident notification is snoozed
 * @param occurredOn   the postponement action timestamp
 */
public record AgroclimaticIncidentPostponedEvent(
        String incidentId,
        String plotId,
        Instant snoozedUntil,
        Instant occurredOn
) {
    public AgroclimaticIncidentPostponedEvent {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incident.event.incident_id.null");
        }
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("incident.event.plot_id.null");
        }
        if (snoozedUntil == null) {
            throw new IllegalArgumentException("incident.event.snoozed_until.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("incident.event.occurred_on.null");
        }
    }
}

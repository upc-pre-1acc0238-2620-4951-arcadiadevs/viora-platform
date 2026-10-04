package com.arcadiadevs.viora.platform.telemetry.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when an agroclimatic incident returns to normal conditions and is resolved.
 *
 * @param incidentId             the incident UUID string
 * @param plotId                 the plot UUID string
 * @param stressDurationMinutes the duration of stress in minutes
 * @param occurredOn             the resolution instant
 */
public record AgroclimaticIncidentNormalizedEvent(
        String incidentId,
        String plotId,
        Long stressDurationMinutes,
        Instant occurredOn
) {
    public AgroclimaticIncidentNormalizedEvent {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incident.event.incident_id.null");
        }
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("incident.event.plot_id.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("incident.event.occurred_on.null");
        }
    }
}

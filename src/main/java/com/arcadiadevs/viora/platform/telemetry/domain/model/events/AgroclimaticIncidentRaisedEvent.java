package com.arcadiadevs.viora.platform.telemetry.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when an agroclimatic incident is raised.
 *
 * @param incidentId the incident UUID string
 * @param plotId     the plot UUID string
 * @param type       the incident type string
 * @param severity   the incident severity string
 * @param occurredOn the instant of occurrence
 */
public record AgroclimaticIncidentRaisedEvent(
        String incidentId,
        String plotId,
        String type,
        String severity,
        Instant occurredOn
) {
    public AgroclimaticIncidentRaisedEvent {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incident.event.incident_id.null");
        }
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("incident.event.plot_id.null");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("incident.event.type.null");
        }
        if (severity == null || severity.isBlank()) {
            throw new IllegalArgumentException("incident.event.severity.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("incident.event.occurred_on.null");
        }
    }
}

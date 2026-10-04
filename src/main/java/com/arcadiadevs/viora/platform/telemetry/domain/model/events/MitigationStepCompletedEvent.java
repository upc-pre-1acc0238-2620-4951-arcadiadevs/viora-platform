package com.arcadiadevs.viora.platform.telemetry.domain.model.events;

import java.time.Instant;

/**
 * Domain event published when an actionable mitigation step is completed.
 *
 * @param incidentId the parent incident UUID string
 * @param stepId     the completed step UUID string
 * @param occurredOn the completion instant
 */
public record MitigationStepCompletedEvent(
        String incidentId,
        String stepId,
        Instant occurredOn
) {
    public MitigationStepCompletedEvent {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incident.event.incident_id.null");
        }
        if (stepId == null || stepId.isBlank()) {
            throw new IllegalArgumentException("incident.event.step_id.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("incident.event.occurred_on.null");
        }
    }
}

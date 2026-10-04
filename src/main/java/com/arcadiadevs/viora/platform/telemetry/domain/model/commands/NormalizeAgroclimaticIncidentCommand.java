package com.arcadiadevs.viora.platform.telemetry.domain.model.commands;

import java.time.Instant;

/**
 * Domain command expressing intent to normalize and resolve an active agroclimatic incident.
 *
 * @param incidentId the incident identifier
 * @param resolvedAt optional resolution instant
 */
public record NormalizeAgroclimaticIncidentCommand(
        String incidentId,
        Instant resolvedAt
) {
    public NormalizeAgroclimaticIncidentCommand {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incident.id.null_or_empty");
        }
    }
}

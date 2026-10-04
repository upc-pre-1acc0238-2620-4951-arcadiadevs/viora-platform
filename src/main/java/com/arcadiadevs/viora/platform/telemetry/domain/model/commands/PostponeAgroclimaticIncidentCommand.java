package com.arcadiadevs.viora.platform.telemetry.domain.model.commands;

/**
 * Domain command expressing intent to postpone (snooze) notifications for an active agroclimatic incident.
 *
 * @param incidentId    the incident identifier
 * @param durationHours the number of hours to snooze
 */
public record PostponeAgroclimaticIncidentCommand(
        String incidentId,
        Long durationHours
) {
    public PostponeAgroclimaticIncidentCommand {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incident.id.null_or_empty");
        }
        if (durationHours == null) {
            throw new IllegalArgumentException("incident.postpone.duration.invalid");
        }
    }
}

package com.arcadiadevs.viora.platform.telemetry.domain.model.commands;

/**
 * Domain command expressing intent to complete a mitigation task step within an incident.
 *
 * @param incidentId the parent incident identifier
 * @param stepId     the step identifier
 */
public record CompleteMitigationStepCommand(
        String incidentId,
        String stepId
) {
    public CompleteMitigationStepCommand {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incident.id.null_or_empty");
        }
        if (stepId == null || stepId.isBlank()) {
            throw new IllegalArgumentException("mitigation_step.id.null_or_empty");
        }
    }
}

package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.CompleteMitigationStepCommand;

/**
 * Assembler creating {@link CompleteMitigationStepCommand} from route path parameters.
 */
public final class CompleteMitigationStepCommandFromResourceAssembler {

    private CompleteMitigationStepCommandFromResourceAssembler() {
    }

    /**
     * Converts route parameters into domain command.
     *
     * @param incidentId the incident unique identifier
     * @param stepId     the step unique identifier
     * @return the domain command
     */
    public static CompleteMitigationStepCommand toCommand(String incidentId, String stepId) {
        return new CompleteMitigationStepCommand(incidentId, stepId);
    }
}

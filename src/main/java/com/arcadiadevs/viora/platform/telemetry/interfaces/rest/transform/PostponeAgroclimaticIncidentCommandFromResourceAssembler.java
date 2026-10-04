package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.PostponeAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.PostponeIncidentResource;

/**
 * Assembler transforming postponement REST request resource into domain command.
 */
public final class PostponeAgroclimaticIncidentCommandFromResourceAssembler {

    private PostponeAgroclimaticIncidentCommandFromResourceAssembler() {
    }

    /**
     * Converts route parameters and payload into {@link PostponeAgroclimaticIncidentCommand}.
     *
     * @param incidentId the incident unique identifier string
     * @param resource   the payload resource
     * @return the domain command
     */
    public static PostponeAgroclimaticIncidentCommand toCommand(String incidentId, PostponeIncidentResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("incident.command.null");
        }
        return new PostponeAgroclimaticIncidentCommand(incidentId, resource.durationHours());
    }
}

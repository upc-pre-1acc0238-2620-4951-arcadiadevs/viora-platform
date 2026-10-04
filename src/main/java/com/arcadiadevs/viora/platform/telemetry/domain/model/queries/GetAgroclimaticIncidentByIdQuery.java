package com.arcadiadevs.viora.platform.telemetry.domain.model.queries;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentId;

/**
 * Domain query for retrieving a single incident by its unique identifier.
 *
 * @param incidentId the incident identifier value object
 */
public record GetAgroclimaticIncidentByIdQuery(IncidentId incidentId) {

    /**
     * Compact constructor enforcing non-nullity.
     */
    public GetAgroclimaticIncidentByIdQuery {
        if (incidentId == null) {
            throw new IllegalArgumentException("incident.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor parsing raw UUID string.
     *
     * @param rawIncidentId the raw UUID string
     */
    public GetAgroclimaticIncidentByIdQuery(String rawIncidentId) {
        this(new IncidentId(rawIncidentId));
    }
}

package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the unique identifier of an Agroclimatic Incident.
 *
 * @param incidentId the UUID string identifier
 */
public record IncidentId(String incidentId) {

    /**
     * Default constructor generating a new random UUID.
     */
    public IncidentId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor validating that the incidentId is a valid, non-blank UUID.
     *
     * @throws IllegalArgumentException if the incidentId is null, blank, or malformed
     */
    public IncidentId {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incident.id.null_or_empty");
        }
        try {
            incidentId = UUID.fromString(incidentId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("incident.id.invalid_uuid", exception);
        }
    }
}

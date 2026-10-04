package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Operational lifecycle status of an agroclimatic incident.
 */
public enum IncidentStatus {
    ACTIVE,
    SNOOZED,
    NORMALIZED;

    /**
     * Parses an IncidentStatus from its string representation.
     *
     * @param value the string representation
     * @return the corresponding enum value
     * @throws IllegalArgumentException if null, blank, or invalid
     */
    public static IncidentStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("incident.status.null");
        }
        try {
            return IncidentStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("incident.status.unknown", e);
        }
    }
}

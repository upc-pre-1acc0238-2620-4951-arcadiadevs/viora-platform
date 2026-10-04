package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Severity level of an agroclimatic incident.
 */
public enum IncidentSeverity {
    WARNING,
    CRITICAL;

    /**
     * Parses an IncidentSeverity from its string representation.
     *
     * @param value the string representation
     * @return the corresponding enum value
     * @throws IllegalArgumentException if null, blank, or invalid
     */
    public static IncidentSeverity from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("incident.severity.null");
        }
        try {
            return IncidentSeverity.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("incident.severity.unknown", e);
        }
    }
}

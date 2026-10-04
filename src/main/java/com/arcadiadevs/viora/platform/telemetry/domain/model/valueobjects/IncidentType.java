package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Enumeration representing the agroclimatic incident types originating within the Telemetry bounded context.
 *
 * <p>Strictly scoped to microclimatic and edaphic stresses evaluated
 * from probe readings and weather forecasts.</p>
 */
public enum IncidentType {
    HYDRIC_STRESS,
    HEAT_WAVE,
    FROST_WARNING;

    /**
     * Parses an IncidentType from its string representation.
     *
     * @param value the string representation
     * @return the corresponding enum value
     * @throws IllegalArgumentException if null, blank, or invalid
     */
    public static IncidentType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("incident.type.null");
        }
        try {
            return IncidentType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("incident.type.unknown", e);
        }
    }
}

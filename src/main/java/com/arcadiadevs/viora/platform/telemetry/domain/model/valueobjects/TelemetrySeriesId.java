package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the unique identifier of a Telemetry Series.
 * Encapsulates a valid UUID string and enforces domain identity invariants.
 *
 * @param seriesId the unique series identifier UUID string
 */
public record TelemetrySeriesId(String seriesId) {

    /**
     * Default constructor generating a new random UUID.
     */
    public TelemetrySeriesId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor validating that the identifier is a non-blank, valid UUID string.
     *
     * @throws IllegalArgumentException if the seriesId is null, blank, or malformed
     */
    public TelemetrySeriesId {
        if (seriesId == null || seriesId.isBlank()) {
            throw new IllegalArgumentException("telemetry.series.id.null_or_empty");
        }
        try {
            seriesId = UUID.fromString(seriesId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("telemetry.series.id.invalid_uuid", exception);
        }
    }
}

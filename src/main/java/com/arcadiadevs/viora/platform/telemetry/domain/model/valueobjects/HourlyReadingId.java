package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the unique identifier of an hourly telemetry reading.
 * Encapsulates a valid UUID string and enforces domain identity invariants.
 *
 * @param readingId the unique reading identifier UUID string
 */
public record HourlyReadingId(String readingId) {

    /**
     * Default constructor generating a new random UUID.
     */
    public HourlyReadingId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor validating that the identifier is a non-blank, valid UUID string.
     *
     * @throws IllegalArgumentException if the readingId is null, blank, or malformed
     */
    public HourlyReadingId {
        if (readingId == null || readingId.isBlank()) {
            throw new IllegalArgumentException("telemetry.reading.id.null_or_empty");
        }
        try {
            readingId = UUID.fromString(readingId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("telemetry.reading.id.invalid_uuid", exception);
        }
    }
}

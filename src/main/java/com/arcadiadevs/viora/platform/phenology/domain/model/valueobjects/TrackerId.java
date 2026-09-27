package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

import java.util.UUID;

/**
 * Universal unique identifier for the {@code ChillAccumulationTracker} aggregate root.
 *
 * @param trackerId the string representation of the UUID
 */
public record TrackerId(String trackerId) {

    /**
     * Constructs a newly generated {@link TrackerId} using a random UUID v4.
     */
    public TrackerId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor validating that the trackerId is a non-blank, valid UUID string.
     *
     * @throws IllegalArgumentException if the trackerId is null, blank, or malformed
     */
    public TrackerId {
        if (trackerId == null || trackerId.isBlank()) {
            throw new IllegalArgumentException("phenology.tracker.id.null_or_empty");
        }
        try {
            trackerId = UUID.fromString(trackerId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("phenology.tracker.id.invalid_uuid", exception);
        }
    }
}

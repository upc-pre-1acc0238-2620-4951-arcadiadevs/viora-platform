package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

import java.time.Instant;

/**
 * Value object representing the immutable timestamp when an agroclimatic telemetry sample was captured.
 *
 * @param timestamp the captured timestamp instant
 */
public record ReadingTimestamp(Instant timestamp) {

    /**
     * Compact constructor enforcing non-null invariant.
     *
     * @throws IllegalArgumentException if timestamp is null
     */
    public ReadingTimestamp {
        if (timestamp == null) {
            throw new IllegalArgumentException("telemetry.reading_timestamp.null");
        }
    }
}

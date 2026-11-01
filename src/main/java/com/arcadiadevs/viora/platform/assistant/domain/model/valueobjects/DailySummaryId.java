package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of a stored daily summary.
 *
 * @param value the UUID, lower case
 */
public record DailySummaryId(String value) {

    /**
     * Compact constructor validating that the value is a UUID.
     */
    public DailySummaryId {
        try {
            value = UUID.fromString(value == null ? "" : value.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("assistant.daily_summary.id.invalid", e);
        }
    }

    /**
     * Creates a new random identifier.
     *
     * @return the identifier
     */
    public static DailySummaryId generate() {
        return new DailySummaryId(UUID.randomUUID().toString());
    }
}

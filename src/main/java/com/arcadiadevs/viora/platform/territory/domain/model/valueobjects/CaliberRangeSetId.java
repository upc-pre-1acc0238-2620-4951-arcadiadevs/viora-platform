package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of the caliber ranges of a license.
 *
 * @param caliberRangeSetId the identifier, a lowercase UUID
 */
public record CaliberRangeSetId(String caliberRangeSetId) {

    /**
     * Generates a new random identifier.
     */
    public CaliberRangeSetId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public CaliberRangeSetId {
        if (caliberRangeSetId == null || caliberRangeSetId.isBlank()) {
            throw new IllegalArgumentException("territory.caliber_range_set.id.null_or_empty");
        }
        try {
            caliberRangeSetId = UUID.fromString(caliberRangeSetId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("territory.caliber_range_set.id.invalid");
        }
    }
}

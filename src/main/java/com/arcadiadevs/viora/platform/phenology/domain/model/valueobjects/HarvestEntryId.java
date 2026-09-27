package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

import java.util.UUID;

/**
 * Universal unique identifier for the internal {@code HistoricalHarvestEntry} entity.
 *
 * @param harvestEntryId the string representation of the UUID
 */
public record HarvestEntryId(String harvestEntryId) {

    /**
     * Constructs a newly generated {@link HarvestEntryId} using a random UUID v4.
     */
    public HarvestEntryId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor validating that the harvestEntryId is a non-blank, valid UUID string.
     *
     * @throws IllegalArgumentException if the harvestEntryId is null, blank, or malformed
     */
    public HarvestEntryId {
        if (harvestEntryId == null || harvestEntryId.isBlank()) {
            throw new IllegalArgumentException("phenology.harvest_entry.id.null_or_empty");
        }
        try {
            harvestEntryId = UUID.fromString(harvestEntryId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("phenology.harvest_entry.id.invalid_uuid", exception);
        }
    }
}

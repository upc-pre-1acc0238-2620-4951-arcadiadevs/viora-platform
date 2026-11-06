package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identity of a sector zone of a cooperative license.
 *
 * @param sectorZoneId the identifier as a lowercase UUID string
 */
public record SectorZoneId(String sectorZoneId) {

    /**
     * Generates a new random identifier.
     */
    public SectorZoneId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public SectorZoneId {
        if (sectorZoneId == null || sectorZoneId.isBlank()) {
            throw new IllegalArgumentException("territory.sector_zone.id.null_or_empty");
        }
        try {
            sectorZoneId = UUID.fromString(sectorZoneId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("territory.sector_zone.id.invalid_uuid", exception);
        }
    }
}

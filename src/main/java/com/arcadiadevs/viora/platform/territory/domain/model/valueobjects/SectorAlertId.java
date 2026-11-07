package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identity of an alert about a sector of a cooperative license.
 *
 * @param sectorAlertId the identifier as a lowercase UUID string
 */
public record SectorAlertId(String sectorAlertId) {

    /**
     * Generates a new random identifier.
     */
    public SectorAlertId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public SectorAlertId {
        if (sectorAlertId == null || sectorAlertId.isBlank()) {
            throw new IllegalArgumentException("territory.sector_alert.id.null_or_empty");
        }
        try {
            sectorAlertId = UUID.fromString(sectorAlertId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("territory.sector_alert.id.invalid_uuid", exception);
        }
    }
}

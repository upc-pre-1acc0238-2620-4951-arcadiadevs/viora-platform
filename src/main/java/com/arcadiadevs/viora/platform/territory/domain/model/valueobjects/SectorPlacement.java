package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * Where a member plot is counted by sector: a sector of the list, «Por confirmar», or no sector informed.
 *
 * @param sectorZoneId the sector; null unless assigned
 * @param status       {@code ASSIGNED}, {@code PENDING} or {@code NONE}
 */
public record SectorPlacement(SectorZoneId sectorZoneId, String status) {

    /** A plot «Por confirmar». */
    public static final SectorPlacement PENDING = new SectorPlacement(null, "PENDING");
    /** A plot without a sector informed. */
    public static final SectorPlacement NONE = new SectorPlacement(null, "NONE");

    /**
     * Validates that an assigned placement has its sector and the others do not.
     *
     * @throws IllegalArgumentException if the status is unknown or does not match the sector
     */
    public SectorPlacement {
        var assigned = "ASSIGNED".equals(status);
        if (!assigned && !"PENDING".equals(status) && !"NONE".equals(status) || assigned != (sectorZoneId != null)) {
            throw new IllegalArgumentException("territory.intake.sector.invalid");
        }
    }

    /**
     * A plot in a sector of the list.
     *
     * @param sectorZoneId the sector
     * @return the placement
     */
    public static SectorPlacement assigned(SectorZoneId sectorZoneId) {
        return new SectorPlacement(sectorZoneId, "ASSIGNED");
    }
}

package com.arcadiadevs.viora.platform.territory.domain.model.commands;

/**
 * Command asking to rename a sector zone of the authenticated manager's license.
 *
 * @param managerId    the authenticated technical manager
 * @param sectorZoneId the sector zone
 * @param name         the new name
 */
public record RenameSectorZoneCommand(String managerId, String sectorZoneId, String name) {

    /**
     * Compact constructor enforcing the required fields.
     */
    public RenameSectorZoneCommand {
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
        if (sectorZoneId == null) {
            throw new IllegalArgumentException("territory.sector_zone.id.null_or_empty");
        }
        if (name == null) {
            throw new IllegalArgumentException("territory.sector_zone.name.blank");
        }
    }
}

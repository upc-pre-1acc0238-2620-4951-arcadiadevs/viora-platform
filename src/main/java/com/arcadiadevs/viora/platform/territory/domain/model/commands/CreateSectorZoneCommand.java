package com.arcadiadevs.viora.platform.territory.domain.model.commands;

/**
 * Command asking to add a sector zone to the list of the authenticated manager's license.
 *
 * @param managerId the authenticated technical manager
 * @param name      the name of the sector
 */
public record CreateSectorZoneCommand(String managerId, String name) {

    /**
     * Compact constructor enforcing the required fields.
     */
    public CreateSectorZoneCommand {
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
        if (name == null) {
            throw new IllegalArgumentException("territory.sector_zone.name.blank");
        }
    }
}

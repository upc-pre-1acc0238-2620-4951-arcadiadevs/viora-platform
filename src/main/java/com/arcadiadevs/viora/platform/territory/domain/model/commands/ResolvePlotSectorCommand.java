package com.arcadiadevs.viora.platform.territory.domain.model.commands;

/**
 * Command a manager sends to review a plot «Por confirmar»: add what the producer wrote as a sector, or say it is the
 * same as an existing zone.
 *
 * @param managerId    the authenticated technical manager
 * @param plotId       the plot «Por confirmar»
 * @param sectorZoneId the existing zone it means; null to add the written name as a new sector
 */
public record ResolvePlotSectorCommand(String managerId, String plotId, String sectorZoneId) {

    /**
     * Compact constructor enforcing the required fields.
     */
    public ResolvePlotSectorCommand {
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
        if (plotId == null) {
            throw new IllegalArgumentException("territory.plot.id.null_or_empty");
        }
    }
}

package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * The color of a member plot within its sector, as the alert policy reads it.
 *
 * @param plotId       the plot
 * @param producerId   its owner
 * @param sectorZoneId its sector
 * @param assessment   its color and reasons
 */
public record PlotRisk(PlotId plotId, ProducerId producerId, SectorZoneId sectorZoneId,
                       PlotRiskAssessment assessment) {

    /**
     * Validates that every part is given.
     *
     * @throws IllegalArgumentException if a part is null
     */
    public PlotRisk {
        if (plotId == null || producerId == null || sectorZoneId == null || assessment == null) {
            throw new IllegalArgumentException("territory.sector_alert.plot_risk.incomplete");
        }
    }
}

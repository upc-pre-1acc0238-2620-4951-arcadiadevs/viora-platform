package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * What a member plot settled in a past campaign, counted only if it was settled after the member joined.
 *
 * @param plotId  the plot
 * @param sector  where it is counted
 * @param settled whether it settled a harvest that campaign since its member joined
 * @param greenKg green olives settled, in kilograms
 * @param blackKg black olives settled, in kilograms
 */
public record PlotSettlement(PlotId plotId, SectorPlacement sector, boolean settled, double greenKg,
                             double blackKg) {

    /**
     * Validates the parts.
     *
     * @throws IllegalArgumentException if the plot or its placement is missing, or a weight is negative
     */
    public PlotSettlement {
        if (plotId == null || sector == null) {
            throw new IllegalArgumentException("territory.intake.plot.incomplete");
        }
        if (greenKg < 0 || blackKg < 0) {
            throw new IllegalArgumentException("territory.intake.plot.invalid");
        }
    }
}

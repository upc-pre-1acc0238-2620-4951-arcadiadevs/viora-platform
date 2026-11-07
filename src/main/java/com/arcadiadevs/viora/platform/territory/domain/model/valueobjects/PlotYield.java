package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * What a member plot brings to the intake projection of a campaign in progress.
 *
 * @param plotId      the plot
 * @param sector      where it is counted
 * @param sampled     whether any tree of it was sampled this campaign
 * @param projectedKg the harvest it is expected to give; null when it cannot be estimated
 * @param greenShare  the share of green olives in its settled history, from 0 to 1; null without history
 */
public record PlotYield(PlotId plotId, SectorPlacement sector, boolean sampled, Double projectedKg,
                        Double greenShare) {

    /**
     * Validates the parts.
     *
     * @throws IllegalArgumentException if the plot or its placement is missing, a value is negative, or the share is
     *                                  outside 0 to 1
     */
    public PlotYield {
        if (plotId == null || sector == null) {
            throw new IllegalArgumentException("territory.intake.plot.incomplete");
        }
        if (projectedKg != null && projectedKg < 0 || greenShare != null && (greenShare < 0 || greenShare > 1)) {
            throw new IllegalArgumentException("territory.intake.plot.invalid");
        }
    }

    /**
     * Tells whether the plot can be estimated, so it adds to the projection.
     *
     * @return true when it has a projected harvest
     */
    public boolean estimable() {
        return projectedKg != null;
    }
}

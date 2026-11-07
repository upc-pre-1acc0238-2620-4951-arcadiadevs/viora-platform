package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.util.List;

/**
 * The intake of a campaign: projected while it is in progress, or settled once it is past.
 *
 * @param status        how reliable it is
 * @param totalKg       the kilograms; null when there is no figure
 * @param greenKg       green olives, by each plot's history
 * @param blackKg       black olives, by each plot's history
 * @param unclassifiedKg kilograms of plots without history, «Sin clasificar»
 * @param plots         the member plots counted
 * @param sampledPlots  those sampled this campaign
 * @param estimablePlots those that add a figure
 * @param sectors       the intake by sector
 */
public record IntakeProjection(IntakeStatus status, Double totalKg, double greenKg, double blackKg,
                               double unclassifiedKg, int plots, int sampledPlots, int estimablePlots,
                               List<SectorIntake> sectors) {

    /**
     * Copies the sectors.
     */
    public IntakeProjection {
        sectors = sectors == null ? List.of() : List.copyOf(sectors);
    }

    /**
     * The share of plots sampled, in percent.
     *
     * @return the coverage, from 0 to 100; 0 without plots
     */
    public double coveragePercent() {
        return plots == 0 ? 0.0 : Math.round(sampledPlots * 1000.0 / plots) / 10.0;
    }
}

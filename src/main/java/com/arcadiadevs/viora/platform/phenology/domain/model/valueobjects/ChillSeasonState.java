package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

/**
 * Biological and operational state of the winter chilling accumulation cycle for an olive plot.
 *
 * <p>State lifecycle:
 * <ul>
 *   <li>{@code IN_PROGRESS}: The plot is within the winter chilling period (June 1 - August 31 in Tacna)
 *       and cumulative chilling portions have not yet reached the varietal threshold (typically 27.0 portions).</li>
 *   <li>{@code COMPLETED}: The cumulative chilling portions have fulfilled or exceeded the varietal physiological requirement.</li>
 *   <li>{@code OFF_SEASON}: The current date or campaign evaluation is outside the active winter chilling window.</li>
 * </ul>
 * </p>
 */
public enum ChillSeasonState {
    IN_PROGRESS,
    COMPLETED,
    OFF_SEASON
}

package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * The confidence of a projection and what is left to sample to reach the next level.
 *
 * @param level           the confidence, by the lower of the plot and hectare coverage
 * @param coveragePercent that lower coverage, in percent
 * @param pendingPlots    plots left to sample for the next level; 0 when the confidence is high
 * @param pendingHectares hectares left to sample for the next level; 0 when the confidence is high
 */
public record IntakeConfidence(ConfidenceLevel level, double coveragePercent, int pendingPlots,
                               double pendingHectares) {
}

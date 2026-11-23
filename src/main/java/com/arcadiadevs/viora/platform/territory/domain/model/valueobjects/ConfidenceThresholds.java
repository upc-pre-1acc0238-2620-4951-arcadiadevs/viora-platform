package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * Sampling coverage, in percent, from which a projection reaches each confidence level.
 *
 * @param mediumPercent the coverage from which confidence is medium
 * @param highPercent   the coverage from which confidence is high
 */
public record ConfidenceThresholds(double mediumPercent, double highPercent) {

    /**
     * Validates the thresholds.
     *
     * @throws IllegalArgumentException unless {@code 0 < mediumPercent < highPercent <= 100}
     */
    public ConfidenceThresholds {
        if (!(mediumPercent > 0) || !(highPercent > mediumPercent) || highPercent > 100) {
            throw new IllegalArgumentException("territory.intake.confidence.thresholds.invalid");
        }
    }
}

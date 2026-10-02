package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * Immutable value object representing a single tree observation exposed by the detailed sampling view.
 *
 * @param roundId          sampling round identifier
 * @param treeTag          physical tree identifier
 * @param shootCount       number of evaluated shoots
 * @param fruitSetCount    number of observed set fruits
 * @param trunkDiameterMm  trunk diameter in millimeters
 * @param samplingDate     date of the field observation
 */
public record SamplingTreeObservation(
        RoundId roundId,
        TreeTag treeTag,
        int shootCount,
        int fruitSetCount,
        double trunkDiameterMm,
        LocalDate samplingDate
) {

    /**
     * Compact constructor enforcing required observation values.
     *
     * @param roundId         sampling round identifier
     * @param treeTag         physical tree identifier
     * @param shootCount      number of evaluated shoots
     * @param fruitSetCount   number of observed set fruits
     * @param trunkDiameterMm trunk diameter in millimeters
     * @param samplingDate    date of the field observation
     */
    public SamplingTreeObservation {
        if (roundId == null) {
            throw new IllegalArgumentException("thinning.round.id.null_or_empty");
        }
        if (treeTag == null) {
            throw new IllegalArgumentException("thinning.tree_tag.null_or_empty");
        }
        if (shootCount <= 0) {
            throw new IllegalArgumentException("thinning.shoot_count.positive");
        }
        if (fruitSetCount < 0) {
            throw new IllegalArgumentException("thinning.fruit_count.negative");
        }
        if (!Double.isFinite(trunkDiameterMm) || trunkDiameterMm <= 0.0) {
            throw new IllegalArgumentException("thinning.trunk_diameter.positive");
        }
        if (samplingDate == null) {
            throw new IllegalArgumentException("thinning.sampling_date.null");
        }
    }
}

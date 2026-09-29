package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * Value Object encapsulating sustainable crop load advisory parameters.
 *
 * @param targetFruitsPerMeter recommended linear fruit density per meter of canopy
 * @param percentageToRemove   percentage of green fruits to manually remove
 * @param windowClosesOn       estimated cutoff date before pit hardening
 */
public record SustainableCropLoad(
        Double targetFruitsPerMeter,
        Double percentageToRemove,
        LocalDate windowClosesOn
) {

    /**
     * Compact constructor validating advisory metrics.
     */
    public SustainableCropLoad {
        if (targetFruitsPerMeter != null && targetFruitsPerMeter <= 0.0) {
            throw new IllegalArgumentException("thinning.target_fruits.positive");
        }
        if (percentageToRemove != null && (percentageToRemove < 0.0 || percentageToRemove > 100.0)) {
            throw new IllegalArgumentException("thinning.percentage_remove.range");
        }
    }

    /**
     * Creates an empty/uncalculated sustainable load placeholder for initial sampling.
     *
     * @return empty SustainableCropLoad
     */
    public static SustainableCropLoad empty() {
        return new SustainableCropLoad(null, null, null);
    }
}

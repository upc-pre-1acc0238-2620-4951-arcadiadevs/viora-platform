package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * Value Object encapsulating sustainable crop load advisory parameters and what they were based on.
 *
 * @param targetFruitsPerShoot recommended sustainable fruits per sampled shoot
 * @param percentageToRemove   percentage of green fruits to manually remove
 * @param windowOpensOn        first day on which thinning is recommended
 * @param windowClosesOn       last recommended day, before pit hardening
 * @param profileVersion       version of the technical profile that supplied the target and the window
 * @param profileStatus        approval status of that profile (for example {@code AGRONOMIST_APPROVED})
 */
public record SustainableCropLoad(
        Double targetFruitsPerShoot,
        Double percentageToRemove,
        LocalDate windowOpensOn,
        LocalDate windowClosesOn,
        String profileVersion,
        String profileStatus
) {

    /**
     * Compact constructor validating advisory metrics.
     */
    public SustainableCropLoad {
        if (targetFruitsPerShoot != null && targetFruitsPerShoot <= 0.0) {
            throw new IllegalArgumentException("thinning.target_fruits.positive");
        }
        if (percentageToRemove != null && (percentageToRemove < 0.0 || percentageToRemove > 100.0)) {
            throw new IllegalArgumentException("thinning.percentage_remove.range");
        }
        if (windowOpensOn != null && windowClosesOn != null && windowOpensOn.isAfter(windowClosesOn)) {
            throw new IllegalArgumentException("thinning.window.invalid");
        }
    }

    /**
     * Creates an empty/uncalculated sustainable load placeholder for initial sampling.
     *
     * @return empty SustainableCropLoad
     */
    public static SustainableCropLoad empty() {
        return new SustainableCropLoad(null, null, null, null, null, null);
    }

    /**
     * Returns the same advisory tagged with the profile it was based on.
     *
     * @param version approved profile version
     * @param status  approval status of the profile
     * @return the advisory with its provenance
     */
    public SustainableCropLoad basedOn(String version, String status) {
        return new SustainableCropLoad(targetFruitsPerShoot, percentageToRemove, windowOpensOn, windowClosesOn,
                version, status);
    }
}

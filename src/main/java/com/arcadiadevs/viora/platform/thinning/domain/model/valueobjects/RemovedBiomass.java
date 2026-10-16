package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/** Actual removal percentage, including zero, and the removed biomass when it was weighed (null otherwise). */
public record RemovedBiomass(Double removedKg, Double actualRemovalPercentage) {
    public RemovedBiomass {
        if (removedKg != null && (!Double.isFinite(removedKg) || removedKg < 0)) {
            throw new IllegalArgumentException("thinning.execution.kg.invalid");
        }
        if (actualRemovalPercentage == null || !Double.isFinite(actualRemovalPercentage)
                || actualRemovalPercentage < 0 || actualRemovalPercentage > 100) {
            throw new IllegalArgumentException("thinning.execution.percentage.invalid");
        }
    }
}

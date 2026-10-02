package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/** Measured removed biomass and actual removal percentage, including zero. */
public record RemovedBiomass(Double removedKg, Double actualRemovalPercentage) {
    public RemovedBiomass {
        if (removedKg == null || !Double.isFinite(removedKg) || removedKg < 0) {
            throw new IllegalArgumentException("thinning.execution.kg.invalid");
        }
        if (actualRemovalPercentage == null || !Double.isFinite(actualRemovalPercentage)
                || actualRemovalPercentage < 0 || actualRemovalPercentage > 100) {
            throw new IllegalArgumentException("thinning.execution.percentage.invalid");
        }
    }
}

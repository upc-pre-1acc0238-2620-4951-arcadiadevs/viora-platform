package com.arcadiadevs.viora.platform.thinning.domain.services;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SustainableCropLoad;

import java.time.LocalDate;

/**
 * Pure domain service calculating thinning removal according to the sustainable crop-load curve.
 *
 * <p>The target threshold is supplied by an approved technical profile instead of being hardcoded in this
 * service. The removal brings the observed load down to the target, both in fruits per shoot:
 * {@code p = 100 * max(0, 1 - target / load)}. No other coefficient is applied.</p>
 */
public final class CropLoadBalancingCalculatorService {

    private final double targetThreshold;

    /**
     * Creates the calculator with a calibrated target fruit density.
     *
     * @param targetThreshold calibrated target fruits per shoot
     */
    public CropLoadBalancingCalculatorService(double targetThreshold) {
        if (!Double.isFinite(targetThreshold) || targetThreshold <= 0.0) {
            throw new IllegalArgumentException("thinning.target_threshold.invalid");
        }
        this.targetThreshold = targetThreshold;
    }

    /**
     * Calculates the recommended sustainable crop load.
     *
     * @param currentFruitsPerShoot current measured fruits per shoot
     * @param windowOpensOn           first recommended thinning date
     * @param windowClosesOn          latest recommended thinning date
     * @return sustainable crop load recommendation
     */
    public SustainableCropLoad calculate(
            double currentFruitsPerShoot,
            LocalDate windowOpensOn,
            LocalDate windowClosesOn
    ) {
        if (!Double.isFinite(currentFruitsPerShoot) || currentFruitsPerShoot < 0.0) {
            throw new IllegalArgumentException("thinning.current_fruits_per_shoot.invalid");
        }

        if (currentFruitsPerShoot == 0.0) {
            return new SustainableCropLoad(targetThreshold, 0.0, windowOpensOn, windowClosesOn, null, null);
        }

        // Full precision: only the REST layer rounds.
        double removalPercentage = 100.0 * Math.max(0.0, 1.0 - targetThreshold / currentFruitsPerShoot);

        return new SustainableCropLoad(
                targetThreshold,
                removalPercentage,
                windowOpensOn,
                windowClosesOn,
                null,
                null
        );
    }

    /**
     * Returns the calibrated target threshold used by this calculator.
     *
     * @return target fruits per shoot
     */
    public double targetThreshold() {
        return targetThreshold;
    }
}

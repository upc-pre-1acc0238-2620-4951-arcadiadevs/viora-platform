package com.arcadiadevs.viora.platform.thinning.domain.services;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SustainableCropLoad;

import java.time.LocalDate;

/**
 * Pure domain service calculating thinning removal according to the sustainable crop-load curve.
 *
 * <p>The target threshold is supplied by agronomic calibration instead of being hardcoded in this service.
 * The BBI modulation is applied as defined by the thinning domain rule.</p>
 */
public final class CropLoadBalancingCalculatorService {

    private final double targetThreshold;

    /**
     * Creates the calculator with a calibrated target fruit density.
     *
     * @param targetThreshold calibrated target fruits per linear canopy meter
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
     * @param currentFruitsPerMeter current measured fruit density per meter
     * @param bbi                     historical Biennial Bearing Index in [0, 1]
     * @param windowClosesOn          latest recommended thinning date
     * @return sustainable crop load recommendation
     */
    public SustainableCropLoad calculate(
            double currentFruitsPerMeter,
            double bbi,
            LocalDate windowClosesOn
    ) {
        if (!Double.isFinite(currentFruitsPerMeter) || currentFruitsPerMeter < 0.0) {
            throw new IllegalArgumentException("thinning.current_fruits_per_meter.invalid");
        }
        if (!Double.isFinite(bbi) || bbi < 0.0 || bbi > 1.0) {
            throw new IllegalArgumentException("thinning.bbi.invalid_range");
        }

        if (currentFruitsPerMeter == 0.0) {
            return new SustainableCropLoad(targetThreshold, 0.0, windowClosesOn);
        }

        double rawRemoval = ((currentFruitsPerMeter - targetThreshold) / currentFruitsPerMeter)
                * 100.0
                * (1.0 + 0.3 * bbi);
        double removalPercentage = Math.max(0.0, Math.min(100.0, rawRemoval));
        removalPercentage = Math.round(removalPercentage * 100.0) / 100.0;

        return new SustainableCropLoad(
                targetThreshold,
                removalPercentage,
                windowClosesOn
        );
    }

    /**
     * Returns the calibrated target threshold used by this calculator.
     *
     * @return target fruits per meter
     */
    public double targetThreshold() {
        return targetThreshold;
    }
}

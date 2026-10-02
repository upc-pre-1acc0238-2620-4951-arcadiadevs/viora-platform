package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Crop load balance after thinning: how much fruit was on the trees, how much remains and how it compares
 * with the sustainable target.
 *
 * <p>Formula: {@code residual = preThinning x (1 - actualRemovalPercentage / 100)},
 * {@code delta = residual - target} and {@code loadRatio = residual / target}.</p>
 *
 * @param preThinningFruitsPerMeter mean fruit density measured by field sampling before thinning
 * @param residualFruitsPerMeter    estimated fruit density left after the actual removal
 * @param targetFruitsPerMeter      sustainable target density of the prescription
 * @param deltaFruitsPerMeter       residual minus target (negative means below the target)
 * @param loadRatio                 residual divided by target
 * @param loadState                 qualitative state derived from the load ratio
 */
public record LoadBalance(
        double preThinningFruitsPerMeter,
        double residualFruitsPerMeter,
        double targetFruitsPerMeter,
        double deltaFruitsPerMeter,
        double loadRatio,
        LoadState loadState
) {

    public LoadBalance {
        if (!Double.isFinite(preThinningFruitsPerMeter) || preThinningFruitsPerMeter < 0.0
                || !Double.isFinite(residualFruitsPerMeter) || residualFruitsPerMeter < 0.0) {
            throw new IllegalArgumentException("thinning.load_balance.load.invalid");
        }
        if (!Double.isFinite(targetFruitsPerMeter) || targetFruitsPerMeter <= 0.0) {
            throw new IllegalArgumentException("thinning.target_fruits.positive");
        }
        if (!Double.isFinite(deltaFruitsPerMeter) || !Double.isFinite(loadRatio) || loadRatio < 0.0
                || loadState == null) {
            throw new IllegalArgumentException("thinning.load_balance.ratio.invalid");
        }
    }

    /**
     * Calculates the balance left by a thinning labor.
     *
     * @param preThinningFruitsPerMeter mean sampled fruit density before thinning
     * @param actualRemovalPercentage   percentage of fruits actually removed, in [0, 100]
     * @param targetFruitsPerMeter      sustainable target density
     * @return the resulting load balance
     */
    public static LoadBalance of(double preThinningFruitsPerMeter, double actualRemovalPercentage,
            double targetFruitsPerMeter) {
        if (!Double.isFinite(actualRemovalPercentage) || actualRemovalPercentage < 0.0
                || actualRemovalPercentage > 100.0) {
            throw new IllegalArgumentException("thinning.execution.percentage.invalid");
        }
        if (!Double.isFinite(targetFruitsPerMeter) || targetFruitsPerMeter <= 0.0) {
            throw new IllegalArgumentException("thinning.target_fruits.positive");
        }
        // Full precision: the load state is decided on these values; rounding belongs to the presentation layer
        double residual = preThinningFruitsPerMeter * (1.0 - actualRemovalPercentage / 100.0);
        double ratio = residual / targetFruitsPerMeter;
        return new LoadBalance(preThinningFruitsPerMeter, residual, targetFruitsPerMeter,
                residual - targetFruitsPerMeter, ratio, LoadState.fromRatio(ratio));
    }
}

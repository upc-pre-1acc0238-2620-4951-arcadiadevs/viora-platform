package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Crop load balance after thinning: how much fruit was on the trees, how much remains and how it compares
 * with the sustainable target.
 *
 * <p>Formula: {@code residual = preThinning x (1 - actualRemovalPercentage / 100)},
 * {@code delta = residual - target} and {@code loadRatio = residual / target}.</p>
 *
 * @param preThinningFruitsPerShoot mean fruit density measured by field sampling before thinning
 * @param residualFruitsPerShoot    estimated fruit density left after the actual removal
 * @param targetFruitsPerShoot      sustainable target density of the prescription
 * @param deltaFruitsPerShoot       residual minus target (negative means below the target)
 * @param loadRatio                 residual divided by target
 * @param loadState                 qualitative state derived from the load ratio
 */
public record LoadBalance(
        double preThinningFruitsPerShoot,
        double residualFruitsPerShoot,
        double targetFruitsPerShoot,
        double deltaFruitsPerShoot,
        double loadRatio,
        LoadState loadState
) {

    public LoadBalance {
        if (!Double.isFinite(preThinningFruitsPerShoot) || preThinningFruitsPerShoot < 0.0
                || !Double.isFinite(residualFruitsPerShoot) || residualFruitsPerShoot < 0.0) {
            throw new IllegalArgumentException("thinning.load_balance.load.invalid");
        }
        if (!Double.isFinite(targetFruitsPerShoot) || targetFruitsPerShoot <= 0.0) {
            throw new IllegalArgumentException("thinning.target_fruits.positive");
        }
        if (!Double.isFinite(deltaFruitsPerShoot) || !Double.isFinite(loadRatio) || loadRatio < 0.0
                || loadState == null) {
            throw new IllegalArgumentException("thinning.load_balance.ratio.invalid");
        }
    }

    /**
     * Calculates the balance left by a thinning labor.
     *
     * @param preThinningFruitsPerShoot mean sampled fruit density before thinning
     * @param actualRemovalPercentage   percentage of fruits actually removed, in [0, 100]
     * @param targetFruitsPerShoot      sustainable target density
     * @return the resulting load balance
     */
    public static LoadBalance of(double preThinningFruitsPerShoot, double actualRemovalPercentage,
            double targetFruitsPerShoot) {
        if (!Double.isFinite(actualRemovalPercentage) || actualRemovalPercentage < 0.0
                || actualRemovalPercentage > 100.0) {
            throw new IllegalArgumentException("thinning.execution.percentage.invalid");
        }
        if (!Double.isFinite(targetFruitsPerShoot) || targetFruitsPerShoot <= 0.0) {
            throw new IllegalArgumentException("thinning.target_fruits.positive");
        }
        // Full precision: the load state is decided on these values; rounding belongs to the presentation layer
        double residual = preThinningFruitsPerShoot * (1.0 - actualRemovalPercentage / 100.0);
        double ratio = residual / targetFruitsPerShoot;
        return new LoadBalance(preThinningFruitsPerShoot, residual, targetFruitsPerShoot,
                residual - targetFruitsPerShoot, ratio, LoadState.fromRatio(ratio));
    }
}

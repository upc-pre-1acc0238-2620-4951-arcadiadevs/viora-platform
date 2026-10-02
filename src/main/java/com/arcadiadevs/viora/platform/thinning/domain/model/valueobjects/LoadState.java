package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Qualitative state of the crop load left on the trees after thinning, relative to the sustainable target.
 *
 * <ul>
 *     <li>{@link #BALANCED}: residual load at or below the sustainable target (ratio &le; 1.0).</li>
 *     <li>{@link #MODERATE_OVERLOAD}: residual load up to 30% above the target (1.0 &lt; ratio &le; 1.3).</li>
 *     <li>{@link #SEVERE_OVERLOAD}: residual load more than 30% above the target (ratio &gt; 1.3),
 *     the "sobrecarga severa" rule of the crop-load requirements.</li>
 * </ul>
 */
public enum LoadState {
    BALANCED,
    MODERATE_OVERLOAD,
    SEVERE_OVERLOAD;

    /** Upper ratio bound of a balanced load. */
    public static final double BALANCED_MAX_RATIO = 1.0;

    /** Upper ratio bound of a moderate overload; above it the overload is severe (more than 30%). */
    public static final double MODERATE_OVERLOAD_MAX_RATIO = 1.3;

    /**
     * Classifies a residual-to-target load ratio.
     *
     * @param loadRatio residual load divided by the sustainable target load
     * @return the matching load state
     */
    public static LoadState fromRatio(double loadRatio) {
        if (!Double.isFinite(loadRatio) || loadRatio < 0.0) {
            throw new IllegalArgumentException("thinning.load_balance.ratio.invalid");
        }
        if (loadRatio <= BALANCED_MAX_RATIO) {
            return BALANCED;
        }
        if (loadRatio <= MODERATE_OVERLOAD_MAX_RATIO) {
            return MODERATE_OVERLOAD;
        }
        return SEVERE_OVERLOAD;
    }
}

package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * How the estimated yield accounts for the size of the fruit.
 *
 * <ul>
 *     <li>{@link #CALIBER_ADJUSTED}: the fruit gets smaller as the load grows, by the elasticity that the caliber
 *     model of the variety learned from real harvests.</li>
 *     <li>{@link #FRUIT_COUNT_ONLY}: the variety is not calibrated (or the load lies outside what it was calibrated
 *     with), so the yield follows the number of fruits alone. With more fruit than the target it is a ceiling,
 *     because in reality the fruit gets smaller.</li>
 * </ul>
 */
public enum YieldEstimateBasis {
    CALIBER_ADJUSTED,
    FRUIT_COUNT_ONLY
}

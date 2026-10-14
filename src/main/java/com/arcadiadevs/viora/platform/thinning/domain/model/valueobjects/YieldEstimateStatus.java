package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Whether the campaign yield of a plot could be estimated and, if not, what is missing.
 *
 * <ul>
 *     <li>{@link #AVAILABLE}: both the sustainable and the estimated yield are known.</li>
 *     <li>{@link #LOAD_UNKNOWN}: the sustainable yield is known, but the current crop load is not (the sampling
 *     is not representative yet or the variety has no target load), so the estimated yield is not.</li>
 *     <li>{@link #INSUFFICIENT_HISTORY}: fewer than three earlier campaigns were harvested on the plot.</li>
 *     <li>{@link #AREA_UNKNOWN}: the plot has no usable area.</li>
 * </ul>
 */
public enum YieldEstimateStatus {
    AVAILABLE,
    LOAD_UNKNOWN,
    INSUFFICIENT_HISTORY,
    AREA_UNKNOWN
}

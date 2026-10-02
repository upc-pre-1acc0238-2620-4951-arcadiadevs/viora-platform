package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

/**
 * Outcome of the stabilization curve. Only {@link #EVALUATED} carries an amplitude reduction rate; the other
 * statuses explain why a reduction cannot be demonstrated yet.
 */
public enum StabilizationStatus {
    /** Baseline and managed periods are sufficient: the amplitude reduction rate is computed. */
    EVALUATED,
    /** Fewer than two pairs of consecutive historical campaigns before the first settlement. */
    INSUFFICIENT_BASELINE,
    /** Fewer than two pairs of consecutive settled campaigns under Viora management. */
    INSUFFICIENT_SETTLEMENTS,
    /** The baseline shows no alternation (index 0), so there is no amplitude to reduce. */
    NO_BASELINE_ALTERNATION
}

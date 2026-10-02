package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Outcome of the commercial caliber projection. Only {@link #ESTIMATED} carries numbers; every other
 * status explains why Viora refuses to give a figure instead of showing an unreliable one.
 */
public enum CaliberProjectionStatus {
    /** The variety model is calibrated and the residual load is inside its validated range. */
    ESTIMATED,
    /** Not enough real harvest observations for the plot variety (or the variety is unknown). */
    NOT_CALIBRATED,
    /** Late thinning: the model is calibrated only with on-time executions. */
    NOT_ESTIMATED_LATE,
    /** The residual load lies outside the load range observed during calibration. */
    OUTSIDE_CALIBRATION_RANGE,
    /** No fruit is left on the trees (for example 100% removal), so there is no caliber to project. */
    NOT_APPLICABLE
}

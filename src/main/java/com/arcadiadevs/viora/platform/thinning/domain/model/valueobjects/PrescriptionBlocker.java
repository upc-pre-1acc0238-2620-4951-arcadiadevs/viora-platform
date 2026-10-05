package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * What still prevents a thinning prescription from being issued. The producer is shown exactly which
 * input is missing instead of an invented figure.
 */
public enum PrescriptionBlocker {

    /** Fewer trees than the minimum statistical coverage have been sampled. */
    SAMPLING_NOT_REPRESENTATIVE,

    /** No approved technical profile gives the target load of the plot variety. */
    TARGET_NOT_CONFIGURED,

    /** The full bloom date of the campaign was not recorded, so the window cannot be placed. */
    FULL_BLOOM_MISSING
}

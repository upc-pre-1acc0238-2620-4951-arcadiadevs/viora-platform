package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * Whether a stored answer still matches the data of the plot.
 */
public enum Freshness {

    /** The data the answer was built from has not changed. */
    UP_TO_DATE,

    /** The data changed, or the question cannot be answered anymore; the app offers to ask again. */
    STALE
}

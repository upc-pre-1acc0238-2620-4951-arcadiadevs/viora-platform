package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/** Whether the data an answer was written from is still the data of the plot. */
public enum Freshness {

    /** The data behind the answer has not changed. */
    UP_TO_DATE,

    /** The plot has new data since the answer was written, so the app offers to ask again. */
    STALE
}

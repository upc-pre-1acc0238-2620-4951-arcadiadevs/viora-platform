package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * Why a model text was not shown and the template text was used instead. Kept for the fallback metrics.
 */
public enum TextRejection {
    /** The text is empty. */
    BLANK,
    /** Longer than the answer allows. */
    TOO_LONG,
    /** More sentences than the answer allows. */
    TOO_MANY_SENTENCES,
    /** A number of the text is not among the facts. */
    UNBACKED_NUMBER,
    /** Advice the facts do not allow, such as irrigating or an action that is not offered. */
    FORBIDDEN_PHRASE,
    /** The model did not return the expected JSON. */
    MALFORMED_OUTPUT,
    /** The model call took too long. */
    TIMEOUT,
    /** The model call failed. */
    MODEL_ERROR,
    /** The validation of the model text failed unexpectedly. */
    VALIDATION_ERROR,
    /** The circuit breaker is open after repeated model failures: the model was not called. */
    BREAKER_OPEN
}

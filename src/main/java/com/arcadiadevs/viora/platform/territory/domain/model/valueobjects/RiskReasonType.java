package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * Why a member plot has its color.
 */
public enum RiskReasonType {
    /** The tree carries more fruit than it sustains, by more than the threshold. */
    OVERLOAD,
    /** An open agroclimatic incident of critical severity. */
    CRITICAL_INCIDENT,
    /** Frost forecast at or below the threshold within the window. */
    FROST_FORECAST,
    /** An open agroclimatic incident of warning severity. */
    WARNING_INCIDENT
}

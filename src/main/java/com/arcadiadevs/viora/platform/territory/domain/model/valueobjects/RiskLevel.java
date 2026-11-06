package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * Color of a member plot in the territorial traffic light, in the order a manager visits them.
 */
public enum RiskLevel {
    /** Red: overload above 30 %, a critical incident, or frost forecast. */
    CRITICAL,
    /** Amber: a warning incident. */
    MODERATE,
    /** Grey: no valid sampling this campaign; never counted as green. */
    NO_DATA,
    /** Green: valid sampling, load within the target and no incidents. */
    OPTIMAL
}

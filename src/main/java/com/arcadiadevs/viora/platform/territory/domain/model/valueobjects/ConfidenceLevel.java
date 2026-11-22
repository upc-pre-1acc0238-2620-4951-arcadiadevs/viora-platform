package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * How far an intake projection can be trusted, by how much of the license was sampled.
 */
public enum ConfidenceLevel {
    /** Too little sampled: the figure is only a hint. */
    LOW,
    /** Enough to plan with care. */
    MEDIUM,
    /** Most of the plots and hectares were sampled. */
    HIGH
}

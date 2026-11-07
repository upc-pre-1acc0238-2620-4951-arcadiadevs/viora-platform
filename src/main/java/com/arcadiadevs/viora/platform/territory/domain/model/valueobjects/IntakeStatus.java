package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * How reliable an intake figure is: a projection of the campaign in progress, or the intake settled in a past one.
 */
public enum IntakeStatus {
    /** Projection: no member sampled this campaign, so there is no figure. */
    NO_DATA,
    /** Projection: fewer than half the plots were sampled; wide margin of uncertainty. */
    PRELIMINARY,
    /** Projection: at least half the plots were sampled («Cobertura suficiente»). */
    SUFFICIENT,
    /** Past campaign: some plots settled their harvest, others did not. */
    PARTIAL,
    /** Past campaign: every plot that counts settled its harvest. */
    CLOSED
}

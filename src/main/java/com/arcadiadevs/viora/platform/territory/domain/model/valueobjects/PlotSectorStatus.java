package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * Whether the sector of a plot is one of the license list or a name its producer wrote that the manager has to review.
 */
public enum PlotSectorStatus {
    /** The plot belongs to a sector zone of the license list. */
    ASSIGNED,
    /** The producer wrote a sector the list does not have: «Por confirmar» until the manager reviews it. */
    PENDING
}

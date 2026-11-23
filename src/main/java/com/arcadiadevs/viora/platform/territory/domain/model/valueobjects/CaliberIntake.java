package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * The projected intake of one caliber range.
 *
 * @param range          the range
 * @param greenKg        green olives, by each plot's history
 * @param blackKg        black olives
 * @param unclassifiedKg olives of plots without history
 */
public record CaliberIntake(CaliberRange range, double greenKg, double blackKg, double unclassifiedKg) {

    /**
     * The kilograms of the range.
     *
     * @return green, black and unclassified together
     */
    public double kilograms() {
        return greenKg + blackKg + unclassifiedKg;
    }
}

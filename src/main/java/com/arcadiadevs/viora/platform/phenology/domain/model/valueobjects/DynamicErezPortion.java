package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

/**
 * Value object representing chilling portions accumulated under the Dynamic Model (Fishman &amp; Erez).
 *
 * @param portions the cumulative chilling portions (&ge; 0.0)
 */
public record DynamicErezPortion(Double portions) {

    /**
     * Compact constructor enforcing non-null and non-negative portions invariant.
     *
     * @throws IllegalArgumentException if portions is null or negative
     */
    public DynamicErezPortion {
        if (portions == null) {
            throw new IllegalArgumentException("phenology.erez_portion.null");
        }
        if (portions < 0.0) {
            throw new IllegalArgumentException("phenology.erez_portion.negative");
        }
    }

    /**
     * Factory method for zero accumulated chilling portions.
     *
     * @return a {@link DynamicErezPortion} with 0.0
     */
    public static DynamicErezPortion zero() {
        return new DynamicErezPortion(0.0);
    }
}

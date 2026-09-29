package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Value object representing stem water potential (SWP) in Megapascals (MPa).
 * Typically ranges between -5.00 MPa (severe water deficit) and 0.00 MPa (fully hydrated).
 *
 * @param stemWaterPotentialMpa the stem water potential in MPa
 */
public record StemWaterPotential(Double stemWaterPotentialMpa) {

    public static final double MIN_STEM_WATER_POTENTIAL_MPA = -5.0;
    public static final double MAX_STEM_WATER_POTENTIAL_MPA = 0.0;

    /**
     * Compact constructor validating stem water potential range.
     *
     * @throws IllegalArgumentException if value is out of physical boundary
     */
    public StemWaterPotential {
        if (stemWaterPotentialMpa != null && (stemWaterPotentialMpa < MIN_STEM_WATER_POTENTIAL_MPA || stemWaterPotentialMpa > MAX_STEM_WATER_POTENTIAL_MPA)) {
            throw new IllegalArgumentException("telemetry.stem_water_potential.out_of_range");
        }
    }
}

package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

/**
 * Value object encapsulating harvest yield distribution in kilograms.
 *
 * <p>Invariant rules:
 * <ul>
 *   <li>totalKg must be strictly greater than 0.0.</li>
 *   <li>greenKg and blackKg must be greater than or equal to 0.0.</li>
 *   <li>The sum of greenKg and blackKg cannot exceed totalKg.</li>
 * </ul>
 * </p>
 *
 * @param totalKg the overall fruit mass in kilograms
 * @param greenKg kilograms harvested for green table olives
 * @param blackKg kilograms harvested for black natural olives
 */
public record HarvestYield(
        Double totalKg,
        Double greenKg,
        Double blackKg
) {

    /**
     * Compact constructor enforcing agronomic invariants for harvest yield.
     *
     * @throws IllegalArgumentException if values are missing, negative, or sum is inconsistent
     */
    public HarvestYield {
        if (totalKg == null || totalKg <= 0.0) {
            throw new IllegalArgumentException("phenology.harvest_yield.total_positive");
        }
        var effectiveGreen = (greenKg != null) ? greenKg : 0.0;
        var effectiveBlack = (blackKg != null) ? blackKg : 0.0;

        if (effectiveGreen < 0.0) {
            throw new IllegalArgumentException("phenology.harvest_yield.green_negative");
        }
        if (effectiveBlack < 0.0) {
            throw new IllegalArgumentException("phenology.harvest_yield.black_negative");
        }
        if ((effectiveGreen + effectiveBlack) > totalKg + 0.0001) {
            throw new IllegalArgumentException("phenology.harvest_yield.incoherent_sum");
        }

        greenKg = effectiveGreen;
        blackKg = effectiveBlack;
    }

    /**
     * Factory method creating a single total yield without separation into olive use types.
     *
     * @param totalKg the total kilograms harvested
     * @return a valid {@link HarvestYield} instance
     */
    public static HarvestYield ofTotal(Double totalKg) {
        return new HarvestYield(totalKg, 0.0, 0.0);
    }
}

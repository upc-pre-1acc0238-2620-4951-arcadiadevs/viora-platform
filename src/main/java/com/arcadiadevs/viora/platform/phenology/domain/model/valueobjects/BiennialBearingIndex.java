package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

/**
 * Value object representing Hoblyn's Biennial Bearing Index (BBI).
 * Bounded within [0.00, 1.00] where:
 * <ul>
 *   <li>0.00 represents complete annual production regularity.</li>
 *   <li>1.00 represents extreme biennial bearing / alternation.</li>
 * </ul>
 *
 * @param value the decimal BBI value
 */
public record BiennialBearingIndex(Double value) {

    public static final double MIN_VALUE = 0.00;
    public static final double MAX_VALUE = 1.00;

    /**
     * Compact constructor validating that the BBI value is within the bounded range [0.00, 1.00].
     *
     * @throws IllegalArgumentException if value is null or outside [0.00, 1.00]
     */
    public BiennialBearingIndex {
        if (value == null) {
            throw new IllegalArgumentException("phenology.bbi.invalid_range");
        }
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new IllegalArgumentException("phenology.bbi.invalid_range");
        }
    }

    /**
     * Factory method representing a neutral baseline or zero alternation.
     *
     * @return a {@link BiennialBearingIndex} initialized to 0.0
     */
    public static BiennialBearingIndex zero() {
        return new BiennialBearingIndex(0.0);
    }
}

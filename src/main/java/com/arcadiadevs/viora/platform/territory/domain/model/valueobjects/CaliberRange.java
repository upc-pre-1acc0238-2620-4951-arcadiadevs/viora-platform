package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * A caliber class of the cooperative's plant, in fruits per kilogram: a lower number means bigger fruit.
 *
 * <p>Both ends belong to the range and must fall on the commercial size scale of table olives, from 60 to 2000 fruits
 * per kilogram, the same scale the thinning caliber uses.</p>
 *
 * @param minFruitsPerKg the biggest fruit of the class
 * @param maxFruitsPerKg the smallest fruit of the class
 */
public record CaliberRange(int minFruitsPerKg, int maxFruitsPerKg) {

    /** The biggest fruit on the commercial size scale. */
    public static final int SCALE_START = 60;

    /** The smallest fruit on the commercial size scale. */
    public static final int SCALE_END = 2000;

    /**
     * Validates the range.
     *
     * @throws IllegalArgumentException if an end is off the scale or the range is inverted
     */
    public CaliberRange {
        if (minFruitsPerKg < SCALE_START || maxFruitsPerKg > SCALE_END) {
            throw new IllegalArgumentException("territory.caliber_range.off_scale");
        }
        if (minFruitsPerKg > maxFruitsPerKg) {
            throw new IllegalArgumentException("territory.caliber_range.inverted");
        }
    }

    /**
     * Tells whether a caliber belongs to the range. A caliber between two whole numbers falls in the range whose ends
     * round to it, so consecutive ranges leave no caliber out.
     *
     * @param fruitsPerKg the caliber
     * @return true when it belongs here
     */
    public boolean contains(double fruitsPerKg) {
        var rounded = Math.round(fruitsPerKg);
        return rounded >= minFruitsPerKg && rounded <= maxFruitsPerKg;
    }
}

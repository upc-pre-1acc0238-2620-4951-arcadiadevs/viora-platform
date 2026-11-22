package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * A handful of fruits weighed during a sampling round, to know their caliber: how many fruits make a kilogram.
 *
 * <p>The caliber must fall on the commercial size scale, so a sample whose fruits are too big or too small to be real
 * olives of the campaign is refused.</p>
 *
 * @param fruitCount  the fruits weighed
 * @param weightGrams what they weigh, in grams
 */
public record FruitSample(int fruitCount, double weightGrams) {

    /** The most fruits a sample may hold, so it stays a handful weighed in the field. */
    public static final int MAX_FRUIT_COUNT = 1000;

    /** The heaviest sample accepted, in grams. */
    public static final double MAX_WEIGHT_GRAMS = 10_000;

    /**
     * Validates the sample.
     *
     * @throws IllegalArgumentException if a value is zero, negative or too large, or the caliber is off the scale
     */
    public FruitSample {
        if (fruitCount <= 0 || fruitCount > MAX_FRUIT_COUNT) {
            throw new IllegalArgumentException("thinning.fruit_sample.count.invalid");
        }
        if (!(weightGrams > 0) || weightGrams > MAX_WEIGHT_GRAMS) {
            throw new IllegalArgumentException("thinning.fruit_sample.weight.invalid");
        }
        var fruitsPerKg = fruitCount * 1000 / weightGrams;
        if (fruitsPerKg < CommercialSizeScale.SCALE_START || fruitsPerKg > CommercialSizeScale.MAX_FRUITS_PER_KG) {
            throw new IllegalArgumentException("thinning.fruit_sample.caliber.out_of_range");
        }
    }

    /**
     * The caliber of the sample.
     *
     * @return fruits per kilogram, to one decimal
     */
    public double fruitsPerKg() {
        return Math.round(fruitCount * 10_000 / weightGrams) / 10.0;
    }
}

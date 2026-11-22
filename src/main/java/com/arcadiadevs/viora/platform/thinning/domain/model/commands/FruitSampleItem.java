package com.arcadiadevs.viora.platform.thinning.domain.model.commands;

/**
 * The fruits weighed in a sampling round, as the client sends them; validated when the round is ingested.
 *
 * @param fruitCount  the fruits weighed
 * @param weightGrams what they weigh, in grams
 */
public record FruitSampleItem(Integer fruitCount, Double weightGrams) {

    /**
     * Compact constructor validating strictly non-null arguments.
     */
    public FruitSampleItem {
        if (fruitCount == null) {
            throw new IllegalArgumentException("thinning.fruit_sample.count.invalid");
        }
        if (weightGrams == null) {
            throw new IllegalArgumentException("thinning.fruit_sample.weight.invalid");
        }
    }
}

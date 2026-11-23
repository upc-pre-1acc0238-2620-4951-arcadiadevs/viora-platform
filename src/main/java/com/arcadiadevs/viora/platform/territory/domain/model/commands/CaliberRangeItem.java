package com.arcadiadevs.viora.platform.territory.domain.model.commands;

/**
 * A caliber range as the client sends it; validated when the ranges are replaced.
 *
 * @param minFruitsPerKg the biggest fruit of the class
 * @param maxFruitsPerKg the smallest fruit of the class
 */
public record CaliberRangeItem(Integer minFruitsPerKg, Integer maxFruitsPerKg) {

    /**
     * Compact constructor validating strictly non-null arguments.
     */
    public CaliberRangeItem {
        if (minFruitsPerKg == null || maxFruitsPerKg == null) {
            throw new IllegalArgumentException("territory.caliber_range.incomplete");
        }
    }
}

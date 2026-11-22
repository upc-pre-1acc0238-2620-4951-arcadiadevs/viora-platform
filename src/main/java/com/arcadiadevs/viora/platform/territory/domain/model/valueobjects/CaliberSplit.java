package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.util.List;

/**
 * The projected intake split by the caliber ranges of the plant.
 *
 * @param ranges       each range, in the plant's order; empty when the plant set no ranges
 * @param unsizedKg    estimable plots whose producer weighed no fruits; null when the plant set no ranges
 * @param outOfRangeKg estimable plots whose caliber falls outside every range; null when the plant set no ranges
 */
public record CaliberSplit(List<CaliberIntake> ranges, Double unsizedKg, Double outOfRangeKg) {

    /** Without ranges there is nothing to split by. */
    public static final CaliberSplit NONE = new CaliberSplit(List.of(), null, null);

    /**
     * Copies the ranges.
     */
    public CaliberSplit {
        ranges = ranges == null ? List.of() : List.copyOf(ranges);
    }
}

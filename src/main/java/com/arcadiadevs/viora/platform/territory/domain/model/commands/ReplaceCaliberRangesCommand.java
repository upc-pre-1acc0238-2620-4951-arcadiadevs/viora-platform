package com.arcadiadevs.viora.platform.territory.domain.model.commands;

import java.util.List;

/**
 * Command replacing the caliber ranges of the license the manager is in charge of.
 *
 * @param managerId the signed-in manager
 * @param ranges    the ranges, from the biggest fruit to the smallest; empty clears them
 */
public record ReplaceCaliberRangesCommand(String managerId, List<CaliberRangeItem> ranges) {

    /**
     * Compact constructor validating strictly non-null arguments.
     */
    public ReplaceCaliberRangesCommand {
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
        if (ranges == null) {
            throw new IllegalArgumentException("territory.caliber_range_set.ranges.null");
        }
        ranges = List.copyOf(ranges);
    }
}

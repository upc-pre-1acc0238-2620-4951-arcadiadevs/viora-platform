package com.arcadiadevs.viora.platform.territory.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.territory.domain.model.commands.CaliberRangeItem;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.ReplaceCaliberRangesCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRange;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.CaliberRangeResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.ReplaceCaliberRangesResource;

/**
 * Assembler between the caliber ranges and their public representation.
 */
public final class CaliberRangeResourceAssembler {

    private CaliberRangeResourceAssembler() {
    }

    /**
     * Builds the representation of a range.
     *
     * @param range the range
     * @return its resource
     */
    public static CaliberRangeResource toResource(CaliberRange range) {
        return new CaliberRangeResource(range.minFruitsPerKg(), range.maxFruitsPerKg());
    }

    /**
     * Builds the command that replaces the ranges of the manager's license.
     *
     * @param managerId the signed-in manager
     * @param resource  the new ranges
     * @return the command
     */
    public static ReplaceCaliberRangesCommand toCommandFromResource(String managerId,
                                                                    ReplaceCaliberRangesResource resource) {
        return new ReplaceCaliberRangesCommand(managerId, resource.ranges().stream()
                .map(range -> new CaliberRangeItem(range.minFruitsPerKg(), range.maxFruitsPerKg()))
                .toList());
    }
}

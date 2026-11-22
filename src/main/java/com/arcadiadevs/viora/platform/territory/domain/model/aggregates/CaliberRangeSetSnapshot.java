package com.arcadiadevs.viora.platform.territory.domain.model.aggregates;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRange;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRangeSetId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;

import java.util.List;

/**
 * State of the caliber ranges of a license.
 *
 * @param id        the set
 * @param licenseId the license
 * @param ranges    the ranges, from the biggest fruit to the smallest
 * @param revision  the optimistic lock version
 */
public record CaliberRangeSetSnapshot(CaliberRangeSetId id, LicenseId licenseId, List<CaliberRange> ranges,
                                      Long revision) {

    /**
     * Copies the ranges.
     */
    public CaliberRangeSetSnapshot {
        ranges = ranges == null ? List.of() : List.copyOf(ranges);
    }
}

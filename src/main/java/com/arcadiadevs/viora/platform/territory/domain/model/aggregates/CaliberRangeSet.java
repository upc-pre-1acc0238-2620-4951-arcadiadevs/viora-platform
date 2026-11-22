package com.arcadiadevs.viora.platform.territory.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.territory.domain.exceptions.CaliberRangeConflictException;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRange;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRangeSetId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;

import java.util.List;
import java.util.Optional;

/**
 * Aggregate root of the caliber ranges a license's plant sorts olives by, so the intake splits the same way.
 *
 * <p>The ranges go from the biggest fruit to the smallest, one right after the other: none overlaps the previous one
 * and none leaves a gap after it, so every caliber between the first and the last falls in exactly one. An empty list
 * means the plant set none yet.</p>
 */
public class CaliberRangeSet extends AbstractDomainAggregateRoot<CaliberRangeSet> {

    /** The most ranges a plant sorts by. */
    public static final int MAX_RANGES = 20;

    private final CaliberRangeSetId id;
    private final LicenseId licenseId;
    private List<CaliberRange> ranges;
    private final Long revision;

    private CaliberRangeSet(CaliberRangeSetSnapshot snapshot) {
        this.id = snapshot.id();
        this.licenseId = snapshot.licenseId();
        this.ranges = snapshot.ranges();
        this.revision = snapshot.revision();
    }

    /**
     * Starts the ranges of a license, with none yet.
     *
     * @param licenseId the license
     * @return the empty set
     * @throws IllegalArgumentException if the license is missing
     */
    public static CaliberRangeSet startFor(LicenseId licenseId) {
        if (licenseId == null) {
            throw new IllegalArgumentException("territory.caliber_range_set.license.null");
        }
        return new CaliberRangeSet(new CaliberRangeSetSnapshot(new CaliberRangeSetId(), licenseId, List.of(), 0L));
    }

    /**
     * Rebuilds the ranges from their stored state.
     *
     * @param snapshot the stored state
     * @return the set
     * @throws IllegalArgumentException if the state is incomplete
     */
    public static CaliberRangeSet reconstitute(CaliberRangeSetSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("territory.caliber_range_set.snapshot.null");
        }
        if (snapshot.id() == null || snapshot.licenseId() == null) {
            throw new IllegalArgumentException("territory.caliber_range_set.snapshot.incomplete");
        }
        return new CaliberRangeSet(snapshot);
    }

    /**
     * Replaces every range at once; the ranges in force stay if the new ones do not fit together.
     *
     * @param newRanges the ranges, from the biggest fruit to the smallest
     * @throws IllegalArgumentException      if the list is missing or too long
     * @throws CaliberRangeConflictException if a range overlaps the previous one or leaves a gap after it
     */
    public void replace(List<CaliberRange> newRanges) {
        if (newRanges == null) {
            throw new IllegalArgumentException("territory.caliber_range_set.ranges.null");
        }
        if (newRanges.size() > MAX_RANGES) {
            throw new IllegalArgumentException("territory.caliber_range_set.too_many");
        }
        for (int i = 1; i < newRanges.size(); i++) {
            var previous = newRanges.get(i - 1);
            var current = newRanges.get(i);
            if (current.minFruitsPerKg() <= previous.maxFruitsPerKg()) {
                throw new CaliberRangeConflictException("territory.caliber_range.overlapping", i);
            }
            if (current.minFruitsPerKg() > previous.maxFruitsPerKg() + 1) {
                throw new CaliberRangeConflictException("territory.caliber_range.gap", i);
            }
        }
        this.ranges = List.copyOf(newRanges);
    }

    /**
     * Finds the range a caliber belongs to.
     *
     * @param fruitsPerKg the caliber
     * @return the range, or empty when it falls outside all of them
     */
    public Optional<CaliberRange> rangeOf(double fruitsPerKg) {
        return ranges.stream().filter(range -> range.contains(fruitsPerKg)).findFirst();
    }

    /**
     * Captures the state.
     *
     * @return the snapshot
     */
    public CaliberRangeSetSnapshot snapshot() {
        return new CaliberRangeSetSnapshot(id, licenseId, ranges, revision);
    }
}

package com.arcadiadevs.viora.platform.territory.domain;

import com.arcadiadevs.viora.platform.territory.domain.exceptions.CaliberRangeConflictException;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.CaliberRangeSet;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRange;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Caliber ranges of a license")
class CaliberRangeSetTest {

    private final CaliberRangeSet set = CaliberRangeSet.startFor(new LicenseId(UUID.randomUUID().toString()));

    @Test
    @DisplayName("Consecutive ranges are kept, and each caliber falls in exactly one")
    void consecutiveRanges() {
        set.replace(List.of(new CaliberRange(140, 180), new CaliberRange(181, 220), new CaliberRange(221, 260)));

        assertThat(set.snapshot().ranges()).hasSize(3);
        assertThat(set.rangeOf(180.4)).contains(new CaliberRange(140, 180));
        assertThat(set.rangeOf(180.6)).contains(new CaliberRange(181, 220));
        assertThat(set.rangeOf(300)).isEmpty();
    }

    @Test
    @DisplayName("An overlap or a gap names the range in conflict and keeps the ranges in force")
    void conflictsKeepTheRangesInForce() {
        set.replace(List.of(new CaliberRange(140, 180)));

        assertThatThrownBy(() -> set.replace(List.of(new CaliberRange(140, 180), new CaliberRange(170, 220))))
                .isInstanceOfSatisfying(CaliberRangeConflictException.class, ex -> {
                    assertThat(ex.getMessage()).isEqualTo("territory.caliber_range.overlapping");
                    assertThat(ex.rangeIndex()).isEqualTo(1);
                });
        assertThatThrownBy(() -> set.replace(List.of(new CaliberRange(140, 180), new CaliberRange(190, 220))))
                .isInstanceOfSatisfying(CaliberRangeConflictException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("territory.caliber_range.gap"));
        assertThat(set.snapshot().ranges()).containsExactly(new CaliberRange(140, 180));
    }

    @Test
    @DisplayName("An inverted range or one off the commercial scale cannot exist, and an empty list clears them")
    void invalidRanges() {
        assertThatThrownBy(() -> new CaliberRange(220, 180)).hasMessage("territory.caliber_range.inverted");
        assertThatThrownBy(() -> new CaliberRange(50, 180)).hasMessage("territory.caliber_range.off_scale");
        assertThatThrownBy(() -> new CaliberRange(140, 2100)).hasMessage("territory.caliber_range.off_scale");

        set.replace(List.of(new CaliberRange(140, 180)));
        set.replace(List.of());
        assertThat(set.snapshot().ranges()).isEmpty();
    }
}

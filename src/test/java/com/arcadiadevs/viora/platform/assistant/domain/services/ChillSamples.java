package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillFacts.Season;

import java.time.LocalDate;
import java.util.TreeMap;

/** Winter chill seasons of a plot against the default requirement of 30 portions. */
final class ChillSamples {

    static final double THRESHOLD = 30.0;

    private ChillSamples() {
    }

    /** July 20, 2026: 24.43 of 30 portions, projected for August 4. */
    static Season inProgress() {
        return season(2026, 24.43, "IN_PROGRESS", "PROJECTED", LocalDate.of(2026, 8, 4), null,
                LocalDate.of(2026, 7, 20), 2);
    }

    /** Accumulation halted by a warm spell, but still projected. */
    static Season halted() {
        return season(2026, 24.43, "HALTED", "PROJECTED", LocalDate.of(2026, 8, 4), null,
                LocalDate.of(2026, 7, 20), 6);
    }

    /** At the recent pace the requirement is not reached by August 31. */
    static Season notReachable() {
        return season(2026, 12.05, "IN_PROGRESS", "NOT_REACHABLE_IN_SEASON", null, null,
                LocalDate.of(2026, 7, 20), 9);
    }

    /** Fewer than 14 days evaluated. */
    static Season insufficient() {
        return season(2026, 3.2, "IN_PROGRESS", "INSUFFICIENT_DATA", null, null, LocalDate.of(2026, 6, 10), 0);
    }

    /** The requirement was reached on August 4 and the season goes on. */
    static Season completedInSeason() {
        return season(2026, 31.6, "COMPLETED", "NOT_APPLICABLE", null, LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 10), 2);
    }

    /** The 2026 winter ended short of the requirement. */
    static Season closedShort() {
        return season(2026, 27.45, "OFF_SEASON", "NOT_APPLICABLE", null, null, LocalDate.of(2026, 8, 31), 11);
    }

    /** The 2026 winter ended after reaching the requirement on August 4. */
    static Season closedCompleted() {
        return season(2026, 34.21, "OFF_SEASON", "NOT_APPLICABLE", null, LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 31), 3);
    }

    /** No day of the season evaluated yet (June 1). */
    static Season notEvaluated() {
        return season(2026, 0.0, "IN_PROGRESS", "INSUFFICIENT_DATA", null, null, null, 0);
    }

    /** The 2025 winter, which reached the requirement on August 10. */
    static Season lastWinter() {
        return season(2025, 33.1, "OFF_SEASON", "NOT_APPLICABLE", null, LocalDate.of(2025, 8, 10),
                LocalDate.of(2025, 8, 31), 5);
    }

    /** The 2025 winter, which ended short. */
    static Season lastWinterShort() {
        return season(2025, 26.8, "OFF_SEASON", "NOT_APPLICABLE", null, null, LocalDate.of(2025, 8, 31), 14);
    }

    /** The 2025 winter with data only until July 1, so July 20 of last winter has no figure. */
    static Season lastWinterWithGap() {
        return season(2025, 33.1, "OFF_SEASON", "NOT_APPLICABLE", null, LocalDate.of(2025, 8, 10),
                LocalDate.of(2025, 7, 1), 5);
    }

    static ChillFacts chill(Season current, Season lastWinter) {
        return new ChillFacts(current, lastWinter);
    }

    /**
     * A season whose daily curve grows evenly from June 1 to the last evaluated day, ending at the accumulated
     * portions, so the same date of another winter can be read.
     */
    static Season season(int year, double accumulated, String state, String projection, LocalDate projected,
                         LocalDate completion, LocalDate through, int daysAbove24) {
        var daily = new TreeMap<LocalDate, Double>();
        if (through != null) {
            var start = LocalDate.of(year, 6, 1);
            long days = through.toEpochDay() - start.toEpochDay() + 1;
            for (long day = 0; day < days; day++) {
                daily.put(start.plusDays(day), Math.round(accumulated * (day + 1) / days * 100.0) / 100.0);
            }
        }
        return new Season(year, accumulated, THRESHOLD, state, projection, projected, completion,
                LocalDate.of(year, 8, 31), through, daysAbove24, 24.0, daily);
    }
}

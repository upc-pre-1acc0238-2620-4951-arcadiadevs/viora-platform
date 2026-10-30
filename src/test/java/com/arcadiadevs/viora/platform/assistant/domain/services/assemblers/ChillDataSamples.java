package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillData.Season;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;

import java.time.LocalDate;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.PLOT;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.TODAY;

/** Winter chill seasons of a plot, as Phenology evaluates them, for the WINTER_CHILL tests. */
public final class ChillDataSamples {

    public static final double THRESHOLD = 30.0;

    private ChillDataSamples() {
    }

    /** July 20, 2026: 24.43 of 30 portions, projected for August 4. */
    public static Season inProgress() {
        return season(2026, 24.43, "IN_PROGRESS", "PROJECTED", LocalDate.of(2026, 8, 4), null,
                LocalDate.of(2026, 7, 20), 2);
    }

    /** Accumulation halted by a warm spell, but still projected. */
    public static Season halted() {
        return season(2026, 24.43, "HALTED", "PROJECTED", LocalDate.of(2026, 8, 4), null,
                LocalDate.of(2026, 7, 20), 6);
    }

    /** At the recent pace the requirement is not reached by August 31. */
    public static Season notReachable() {
        return season(2026, 12.05, "IN_PROGRESS", "NOT_REACHABLE_IN_SEASON", null, null,
                LocalDate.of(2026, 7, 20), 9);
    }

    /** Fewer than 14 days evaluated. */
    public static Season insufficient() {
        return season(2026, 3.2, "IN_PROGRESS", "INSUFFICIENT_DATA", null, null, LocalDate.of(2026, 6, 10), 0);
    }

    /** The requirement was reached on August 4 and the season goes on. */
    public static Season completedInSeason() {
        return season(2026, 31.6, "COMPLETED", "NOT_APPLICABLE", null, LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 10), 2);
    }

    /** The 2026 winter ended short of the requirement. */
    public static Season closedShort() {
        return season(2026, 27.45, "OFF_SEASON", "NOT_APPLICABLE", null, null, LocalDate.of(2026, 8, 31), 11);
    }

    /** The 2026 winter ended after reaching the requirement on August 4. */
    public static Season closedCompleted() {
        return season(2026, 34.21, "OFF_SEASON", "NOT_APPLICABLE", null, LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 31), 3);
    }

    /** No day of the season evaluated yet (June 1). */
    public static Season notEvaluated() {
        return season(2026, 0.0, "IN_PROGRESS", "INSUFFICIENT_DATA", null, null, null, 0);
    }

    /** The 2025 winter, which reached the requirement on August 10. */
    public static Season lastWinter() {
        return season(2025, 33.1, "OFF_SEASON", "NOT_APPLICABLE", null, LocalDate.of(2025, 8, 10),
                LocalDate.of(2025, 8, 31), 5);
    }

    /** The 2025 winter, which ended short. */
    public static Season lastWinterShort() {
        return season(2025, 26.8, "OFF_SEASON", "NOT_APPLICABLE", null, null, LocalDate.of(2025, 8, 31), 14);
    }

    public static ChillData chill(Season current, Season lastWinter) {
        return new ChillData(current, lastWinter);
    }

    public static PlotAnswerData data(Season current, Season lastWinter) {
        return PlotAnswerData.ofChill(PLOT, TODAY, TODAY.getYear(), chill(current, lastWinter));
    }

    /**
     * A season whose daily curve grows evenly from June 1 to the last evaluated day, ending at the accumulated
     * portions, so the same date of another winter can be read.
     */
    public static Season season(int year, double accumulated, String state, String projection, LocalDate projected,
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

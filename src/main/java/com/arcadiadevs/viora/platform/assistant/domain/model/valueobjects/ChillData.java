package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Winter chill data of a plot the WINTER_CHILL questions need, as Phenology evaluates it with the Erez dynamic model.
 * The current season is the one the chill metric describes: the winter in progress from June 1 to August 31,
 * otherwise the last finished one.
 *
 * @param current    the season the screen is about; null when Phenology cannot evaluate it
 * @param lastWinter the season before it, over its whole season; null when it cannot be evaluated
 */
public record ChillData(Season current, Season lastWinter) {

    /** Screens that read no chill data. */
    public static final ChillData NONE = new ChillData(null, null);

    /**
     * Whether at least one day of the current season was evaluated, so its figures can be shown.
     *
     * @return true when the current season has data
     */
    public boolean evaluated() {
        return current != null && current.evaluatedThrough() != null;
    }

    /**
     * Whether last winter has data to compare with.
     *
     * @return true when last winter was evaluated
     */
    public boolean lastWinterEvaluated() {
        return lastWinter != null && lastWinter.evaluatedThrough() != null;
    }

    /**
     * One winter chill season.
     *
     * @param seasonYear              the year of the winter
     * @param accumulatedPortions     portions accumulated up to {@code evaluatedThrough}
     * @param thresholdPortions       the varietal chill requirement
     * @param state                   {@code IN_PROGRESS}, {@code HALTED}, {@code COMPLETED} or {@code OFF_SEASON}
     * @param projectionStatus        {@code PROJECTED}, {@code NOT_REACHABLE_IN_SEASON}, {@code INSUFFICIENT_DATA}
     *                                or {@code NOT_APPLICABLE}
     * @param projectedCompletionDate the day the requirement would be reached at the recent pace, or null
     * @param completionDate          the day the requirement was reached, or null
     * @param seasonEnd               August 31 of the winter, the budbreak reference of the screen
     * @param evaluatedThrough        the last day with temperatures, or null
     * @param daysAbove24Celsius      days of the season whose maximum was above {@code warmDayCelsius}
     * @param warmDayCelsius          maximum above which a day counts as warm, °C
     * @param dailyPortions           accumulated portions at the end of each evaluated day
     */
    public record Season(
            int seasonYear,
            double accumulatedPortions,
            double thresholdPortions,
            String state,
            String projectionStatus,
            LocalDate projectedCompletionDate,
            LocalDate completionDate,
            LocalDate seasonEnd,
            LocalDate evaluatedThrough,
            int daysAbove24Celsius,
            double warmDayCelsius,
            SortedMap<LocalDate, Double> dailyPortions
    ) {

        public static final String IN_PROGRESS = "IN_PROGRESS";
        public static final String HALTED = "HALTED";
        public static final String OFF_SEASON = "OFF_SEASON";
        public static final String PROJECTED = "PROJECTED";
        public static final String NOT_REACHABLE_IN_SEASON = "NOT_REACHABLE_IN_SEASON";
        public static final String INSUFFICIENT_DATA = "INSUFFICIENT_DATA";

        public Season {
            if (state == null || projectionStatus == null || seasonEnd == null) {
                throw new IllegalArgumentException("assistant.chill_season.invalid");
            }
            dailyPortions = dailyPortions == null
                    ? Collections.emptySortedMap()
                    : Collections.unmodifiableSortedMap(new TreeMap<>(dailyPortions));
        }

        /**
         * Whether the requirement was reached.
         *
         * @return true with a completion date
         */
        public boolean completed() {
            return completionDate != null;
        }

        /**
         * Whether the season is still accumulating: inside June 1 - August 31 and without the requirement reached.
         *
         * @return true while in progress or halted by a warm spell
         */
        public boolean inProgress() {
            return IN_PROGRESS.equals(state) || HALTED.equals(state);
        }

        /**
         * Whether the season is over (today is after August 31).
         *
         * @return true once the season ended
         */
        public boolean closed() {
            return OFF_SEASON.equals(state);
        }

        /**
         * Portions accumulated by the end of a day.
         *
         * @param date the day
         * @return the portions, or null when the day was not evaluated
         */
        public Double portionsOn(LocalDate date) {
            return date == null ? null : dailyPortions.get(date);
        }
    }
}

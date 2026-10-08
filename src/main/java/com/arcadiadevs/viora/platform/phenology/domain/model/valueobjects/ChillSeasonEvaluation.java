package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

/**
 * Result of evaluating one winter chill season (June 1 - August 31) of a plot with the Dynamic Model (US22),
 * including its warm winter signal (US23).
 *
 * @param seasonYear              the year of the winter
 * @param seasonStart             June 1 of that year
 * @param seasonEnd               August 31 of that year
 * @param evaluatedThrough        the last day with temperatures, or {@code null} when no day has been evaluated
 * @param thresholdPortions       the varietal chill requirement
 * @param accumulatedPortions     the portions accumulated up to {@code evaluatedThrough}
 * @param completionDate          the first day the threshold was reached, or {@code null}
 * @param idleDays                consecutive days, ending at {@code evaluatedThrough}, that added no portion
 * @param seasonState             the state of the season today
 * @param daysAbove24Celsius      days of the season whose maximum was above 24 °C
 * @param currentWarmStreakDays   consecutive warm days ending at {@code evaluatedThrough}
 * @param longestWarmStreakDays   the longest run of consecutive warm days in the season
 * @param thermalAnomaly          the warm winter signal
 * @param projectionStatus        whether a completion date could be projected
 * @param projectedCompletionDate the projected day the threshold will be reached, or {@code null}
 * @param dailyCurve              one point per evaluated day, oldest first
 */
public record ChillSeasonEvaluation(
        int seasonYear,
        LocalDate seasonStart,
        LocalDate seasonEnd,
        @Nullable LocalDate evaluatedThrough,
        double thresholdPortions,
        double accumulatedPortions,
        @Nullable LocalDate completionDate,
        int idleDays,
        ChillSeasonState seasonState,
        int daysAbove24Celsius,
        int currentWarmStreakDays,
        int longestWarmStreakDays,
        ThermalAnomalyStatus thermalAnomaly,
        ChillProjectionStatus projectionStatus,
        @Nullable LocalDate projectedCompletionDate,
        List<DailyChillPoint> dailyCurve
) {

    /**
     * Compact constructor validating mandatory fields.
     */
    public ChillSeasonEvaluation {
        if (seasonStart == null || seasonEnd == null) {
            throw new IllegalArgumentException("phenology.season_start.null");
        }
        if (seasonState == null) {
            throw new IllegalArgumentException("phenology.season_state.null");
        }
        if (thermalAnomaly == null || projectionStatus == null) {
            throw new IllegalArgumentException("phenology.chill_season.status.null");
        }
        dailyCurve = dailyCurve == null ? List.of() : List.copyOf(dailyCurve);
    }

    /**
     * Portions accumulated by the end of the given day, when that day was evaluated.
     *
     * @param date the local date
     * @return the accumulated portions, or {@code null} when that day has no data
     */
    public @Nullable Double portionsOn(LocalDate date) {
        return dailyCurve.stream()
                .filter(point -> point.date().equals(date))
                .map(DailyChillPoint::accumulatedPortions)
                .findFirst()
                .orElse(null);
    }
}

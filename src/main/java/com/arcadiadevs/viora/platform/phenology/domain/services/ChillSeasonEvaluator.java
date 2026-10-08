package com.arcadiadevs.viora.platform.phenology.domain.services;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ChillProjectionStatus;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ChillSeasonEvaluation;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ChillSeasonState;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.DailyChillPoint;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HourlyTemperature;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ThermalAnomalyStatus;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pure domain service that turns the hourly temperatures of a plot into the evaluation of one winter chill
 * season (US22) and its warm winter signal (US23).
 *
 * <p>Only whole past days are evaluated: hours of {@code today} or later, and hours outside June 1 - August 31,
 * are ignored, so a partial day never looks like a day without cold.</p>
 */
public final class ChillSeasonEvaluator {

    private static final Month SEASON_START_MONTH = Month.JUNE;
    private static final int SEASON_START_DAY = 1;
    private static final Month SEASON_END_MONTH = Month.AUGUST;
    private static final int SEASON_END_DAY = 31;
    private static final double MIN_DAILY_GAIN = 0.01;

    private ChillSeasonEvaluator() {
    }

    /**
     * June 1 of the given winter.
     *
     * @param seasonYear the year of the winter
     * @return the first day of the season
     */
    public static LocalDate seasonStart(int seasonYear) {
        return LocalDate.of(seasonYear, SEASON_START_MONTH, SEASON_START_DAY);
    }

    /**
     * August 31 of the given winter.
     *
     * @param seasonYear the year of the winter
     * @return the last day of the season
     */
    public static LocalDate seasonEnd(int seasonYear) {
        return LocalDate.of(seasonYear, SEASON_END_MONTH, SEASON_END_DAY);
    }

    /**
     * The winter the metric describes on a given day: the current one from June 1 on, otherwise the last one.
     *
     * @param today the local date of the plot
     * @return the year of the winter to evaluate
     */
    public static int seasonYearFor(LocalDate today) {
        return today.isBefore(seasonStart(today.getYear())) ? today.getYear() - 1 : today.getYear();
    }

    /**
     * Evaluates one season.
     *
     * @param seasonYear        the year of the winter
     * @param hours             the hourly temperatures of the plot, in any order
     * @param thresholdPortions the varietal chill requirement
     * @param today             the local date of the plot
     * @return the evaluation of the season
     */
    public static ChillSeasonEvaluation evaluate(int seasonYear, List<HourlyTemperature> hours, double thresholdPortions, LocalDate today) {
        var start = seasonStart(seasonYear);
        var end = seasonEnd(seasonYear);
        var curve = dailyCurve(hours, start, end, today);

        var last = curve.isEmpty() ? null : curve.getLast();
        double accumulated = last == null ? 0.0 : last.accumulatedPortions();
        LocalDate completionDate = curve.stream()
                .filter(point -> point.accumulatedPortions() >= thresholdPortions)
                .map(DailyChillPoint::date)
                .findFirst()
                .orElse(null);

        boolean inSeason = !today.isBefore(start) && !today.isAfter(end);
        int currentStreak = currentWarmStreak(curve);
        int longestStreak = longestWarmStreak(curve);
        var anomaly = anomaly(inSeason, currentStreak, longestStreak);
        var state = state(inSeason, completionDate != null, anomaly);

        var projectionStatus = ChillProjectionStatus.NOT_APPLICABLE;
        LocalDate projectedDate = null;
        if (state == ChillSeasonState.IN_PROGRESS || state == ChillSeasonState.HALTED) {
            projectedDate = projectCompletion(curve, thresholdPortions, end);
            projectionStatus = curve.size() < ChillProjectionStatus.PACE_WINDOW_DAYS
                    ? ChillProjectionStatus.INSUFFICIENT_DATA
                    : projectedDate == null ? ChillProjectionStatus.NOT_REACHABLE_IN_SEASON : ChillProjectionStatus.PROJECTED;
        }

        return new ChillSeasonEvaluation(
                seasonYear,
                start,
                end,
                last == null ? null : last.date(),
                thresholdPortions,
                round(accumulated),
                completionDate,
                idleDays(curve),
                state,
                (int) curve.stream().filter(ChillSeasonEvaluator::isWarm).count(),
                currentStreak,
                longestStreak,
                anomaly,
                projectionStatus,
                projectedDate,
                curve
        );
    }

    private static List<DailyChillPoint> dailyCurve(List<HourlyTemperature> hours, LocalDate start, LocalDate end, LocalDate today) {
        Map<LocalDate, List<HourlyTemperature>> byDay = new TreeMap<>();
        hours.stream()
                .filter(hour -> {
                    var date = hour.hour().toLocalDate();
                    return !date.isBefore(start) && !date.isAfter(end) && date.isBefore(today);
                })
                .sorted(Comparator.comparing(HourlyTemperature::hour))
                .forEach(hour -> byDay.computeIfAbsent(hour.hour().toLocalDate(), date -> new ArrayList<>()).add(hour));

        var ordered = byDay.values().stream().flatMap(List::stream).toList();
        double[] cumulative = ErezDynamicModelCalculator.cumulativePortions(
                ordered.stream().map(HourlyTemperature::celsius).toList());

        var curve = new ArrayList<DailyChillPoint>();
        int index = 0;
        for (var day : byDay.entrySet()) {
            index += day.getValue().size();
            double max = day.getValue().stream().mapToDouble(HourlyTemperature::celsius).max().orElse(Double.NaN);
            curve.add(new DailyChillPoint(day.getKey(), round(cumulative[index - 1]), max));
        }
        return curve;
    }

    private static ThermalAnomalyStatus anomaly(boolean inSeason, int currentStreak, int longestStreak) {
        if (inSeason && currentStreak > ThermalAnomalyStatus.WARM_SPELL_MIN_EXCLUSIVE_DAYS) {
            return ThermalAnomalyStatus.ACTIVE;
        }
        if (longestStreak > ThermalAnomalyStatus.WARM_SPELL_MIN_EXCLUSIVE_DAYS) {
            return ThermalAnomalyStatus.RECORDED;
        }
        return ThermalAnomalyStatus.NONE;
    }

    private static ChillSeasonState state(boolean inSeason, boolean completed, ThermalAnomalyStatus anomaly) {
        if (!inSeason) {
            return ChillSeasonState.OFF_SEASON;
        }
        if (completed) {
            return ChillSeasonState.COMPLETED;
        }
        return anomaly == ThermalAnomalyStatus.ACTIVE ? ChillSeasonState.HALTED : ChillSeasonState.IN_PROGRESS;
    }

    private static LocalDate projectCompletion(List<DailyChillPoint> curve, double threshold, LocalDate seasonEnd) {
        int window = ChillProjectionStatus.PACE_WINDOW_DAYS;
        if (curve.size() < window) {
            return null;
        }
        var last = curve.getLast();
        double windowStart = curve.size() > window ? curve.get(curve.size() - 1 - window).accumulatedPortions() : 0.0;
        double pace = (last.accumulatedPortions() - windowStart) / window;
        if (pace <= 0.0) {
            return null;
        }
        long daysNeeded = (long) Math.ceil((threshold - last.accumulatedPortions()) / pace);
        var projected = last.date().plusDays(Math.max(1, daysNeeded));
        return projected.isAfter(seasonEnd) ? null : projected;
    }

    private static int idleDays(List<DailyChillPoint> curve) {
        int idle = 0;
        for (int i = curve.size() - 1; i >= 0; i--) {
            double previous = i == 0 ? 0.0 : curve.get(i - 1).accumulatedPortions();
            if (curve.get(i).accumulatedPortions() - previous >= MIN_DAILY_GAIN) {
                break;
            }
            idle++;
        }
        return idle;
    }

    private static int currentWarmStreak(List<DailyChillPoint> curve) {
        int streak = 0;
        for (int i = curve.size() - 1; i >= 0 && isWarm(curve.get(i)); i--) {
            streak++;
        }
        return streak;
    }

    private static int longestWarmStreak(List<DailyChillPoint> curve) {
        int longest = 0;
        int streak = 0;
        for (var point : curve) {
            streak = isWarm(point) ? streak + 1 : 0;
            longest = Math.max(longest, streak);
        }
        return longest;
    }

    private static boolean isWarm(DailyChillPoint point) {
        return point.maxTemperatureCelsius() > ThermalAnomalyStatus.WARM_DAY_MAX_TEMPERATURE;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

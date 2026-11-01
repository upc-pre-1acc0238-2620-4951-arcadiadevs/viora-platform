package com.arcadiadevs.viora.platform.phenology.interfaces.acl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * ACL facade exposing Phenology bounded context capabilities to external contexts.
 *
 * <p>Only primitive, published-language structures cross the boundary; Phenology aggregates and value
 * objects stay internal.</p>
 */
public interface PhenologyContextFacade {

    /**
     * Retrieves the historical harvest yields registered by the producer for a plot (US20 history).
     *
     * @param plotId the plot identifier string (UUID)
     * @return total kilograms per campaign year in ascending year order; empty when the plot has no history,
     *         does not exist or is not active
     */
    SortedMap<Integer, Double> findHistoricalYields(String plotId);

    /**
     * Retrieves the daily maximum temperature at a plot, from the same hourly weather the chill model reads.
     *
     * @param plotId the plot identifier string (UUID)
     * @param from   first day, inclusive
     * @param to     last day, inclusive
     * @return maximum in degrees Celsius per local day (America/Lima), ascending; empty without location or weather
     */
    SortedMap<LocalDate, Double> findDailyMaxTemperatures(String plotId, LocalDate from, LocalDate to);

    /**
     * Retrieves the latest Hoblyn Biennial Bearing Index of a plot, as kept by its harvest history tracker.
     *
     * @param plotId the plot identifier string (UUID)
     * @return the index and its alternation band; empty when the plot has no tracker or too few campaigns to
     *         classify the index
     */
    Optional<PlotBearingIndex> findBiennialBearingIndex(String plotId);

    /**
     * The winter the chill metric describes today: the current one from June 1 on, otherwise the last one.
     *
     * @return the year of that winter, in the plots' time zone
     */
    int currentChillSeasonYear();

    /**
     * Evaluates one winter chill season of a plot (US22) with the varietal requirement, from the same hourly weather
     * and rules as the chill metric. A past winter is evaluated over its whole season (June 1 - August 31).
     *
     * @param plotId     the plot identifier string (UUID)
     * @param seasonYear the year of the winter; not after {@link #currentChillSeasonYear()}
     * @return the season; empty when the plot is unknown or has no location, the weather is unavailable or the season
     *         has not started
     */
    Optional<PlotChillSeason> findChillSeason(String plotId, int seasonYear);

    /**
     * Biennial Bearing Index of a plot.
     *
     * @param value    index value between 0 and 1
     * @param category alternation band: {@code REGULAR}, {@code MODERATE_ALTERNATION} or {@code SEVERE_ALTERNATION}
     */
    record PlotBearingIndex(double value, String category) {
    }

    /**
     * One winter chill season of a plot, as the chill metric evaluates it.
     *
     * @param seasonYear              the year of the winter
     * @param accumulatedPortions     chill portions accumulated up to {@code evaluatedThrough}, two decimals
     * @param thresholdPortions       the varietal chill requirement
     * @param seasonState             {@code IN_PROGRESS}, {@code HALTED}, {@code COMPLETED} or {@code OFF_SEASON}
     * @param projectionStatus        {@code PROJECTED}, {@code NOT_REACHABLE_IN_SEASON}, {@code INSUFFICIENT_DATA}
     *                                or {@code NOT_APPLICABLE}
     * @param projectedCompletionDate the day the requirement would be reached at the recent pace, or {@code null}
     * @param completionDate          the first day the requirement was reached, or {@code null}
     * @param seasonEnd               August 31 of the winter
     * @param evaluatedThrough        the last day with temperatures, or {@code null} when no day was evaluated
     * @param daysAbove24Celsius      days of the season whose maximum was above {@code warmDayCelsius}
     * @param warmDayCelsius          maximum temperature above which a day counts as warm, °C
     * @param dailyPortions           accumulated portions at the end of each evaluated day, ascending
     */
    record PlotChillSeason(int seasonYear, double accumulatedPortions, double thresholdPortions, String seasonState,
            String projectionStatus, LocalDate projectedCompletionDate, LocalDate completionDate,
            LocalDate seasonEnd, LocalDate evaluatedThrough, int daysAbove24Celsius, double warmDayCelsius,
            SortedMap<LocalDate, Double> dailyPortions) {

        public PlotChillSeason {
            dailyPortions = dailyPortions == null
                    ? Collections.emptySortedMap()
                    : Collections.unmodifiableSortedMap(new TreeMap<>(dailyPortions));
        }
    }
}

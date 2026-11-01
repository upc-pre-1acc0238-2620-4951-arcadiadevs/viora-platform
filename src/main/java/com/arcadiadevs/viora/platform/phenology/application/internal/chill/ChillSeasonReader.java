package com.arcadiadevs.viora.platform.phenology.application.internal.chill;

import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather.HourlyTemperatureProvider;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ChillSeasonEvaluation;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HourlyTemperature;
import com.arcadiadevs.viora.platform.phenology.domain.services.ChillSeasonEvaluator;
import com.arcadiadevs.viora.platform.phenology.domain.services.ErezDynamicModelCalculator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Reads the hourly temperatures of one winter at a location and evaluates it with the varietal chill requirement,
 * as the chill metric (US22) and the other contexts see it. Only whole past days of the season are asked for.
 */
public final class ChillSeasonReader {

    private final HourlyTemperatureProvider temperatureProvider;

    public ChillSeasonReader(HourlyTemperatureProvider temperatureProvider) {
        this.temperatureProvider = temperatureProvider;
    }

    /**
     * Evaluates a winter as it stands on a day.
     *
     * @param latitude   the latitude in decimal degrees
     * @param longitude  the longitude in decimal degrees
     * @param seasonYear the year of the winter
     * @param today      the local date of the plot
     * @return the evaluation, or empty when the weather provider cannot be reached
     */
    public Optional<ChillSeasonEvaluation> evaluate(double latitude, double longitude, int seasonYear,
            LocalDate today) {
        return seasonHours(latitude, longitude, seasonYear, today).map(hours -> ChillSeasonEvaluator.evaluate(
                seasonYear, hours, ErezDynamicModelCalculator.DEFAULT_VARIETAL_CHILL_THRESHOLD, today));
    }

    /**
     * Evaluates a winter over its whole season, as it ended.
     *
     * @param latitude   the latitude in decimal degrees
     * @param longitude  the longitude in decimal degrees
     * @param seasonYear the year of the winter
     * @return the evaluation, or empty when the weather provider cannot be reached
     */
    public Optional<ChillSeasonEvaluation> evaluateWholeSeason(double latitude, double longitude, int seasonYear) {
        return evaluate(latitude, longitude, seasonYear, ChillSeasonEvaluator.seasonEnd(seasonYear).plusDays(1));
    }

    /** The hours from June 1 to yesterday or August 31; no hours at all before the first whole day. */
    private Optional<List<HourlyTemperature>> seasonHours(double latitude, double longitude, int seasonYear,
            LocalDate today) {
        var from = ChillSeasonEvaluator.seasonStart(seasonYear);
        var seasonEnd = ChillSeasonEvaluator.seasonEnd(seasonYear);
        var yesterday = today.minusDays(1);
        var to = yesterday.isBefore(seasonEnd) ? yesterday : seasonEnd;
        if (to.isBefore(from)) {
            return Optional.of(List.of());
        }
        return temperatureProvider.fetchHourlyTemperatures(latitude, longitude, from, to);
    }
}

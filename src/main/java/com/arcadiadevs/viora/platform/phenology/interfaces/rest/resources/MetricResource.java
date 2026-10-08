package com.arcadiadevs.viora.platform.phenology.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

import java.time.Instant;
import java.util.Map;

/**
 * Public REST response resource representing a computed phenological metric for an olive plot.
 *
 * @param metricName          the identifier name of the evaluated metric
 * @param value               the quantitative numerical score or index value
 * @param qualitativeCategory the qualitative biological status or alternation category
 * @param details             detailed formula metadata and parameter map
 * @param evaluatedAt         the timestamp when the metric was calculated
 */
@Schema(
        name = "MetricResource",
        description = "Response resource representing an evaluated phenological metric (e.g. BIENNIAL_BEARING_INDEX or EREZ_CHILLING_PORTIONS)",
        example = "{\"metricName\": \"EREZ_CHILLING_PORTIONS\", \"value\": 18.42, \"qualitativeCategory\": \"DEFICIENT\", \"details\": {\"model\": \"Dynamic Model (Fishman & Erez)\", \"weatherSource\": \"Open-Meteo historical weather at the plot centroid\", \"thresholdTarget\": 30.0, \"completionPercentage\": 61.4, \"seasonYear\": 2026, \"seasonStart\": \"2026-06-01\", \"seasonEnd\": \"2026-08-31\", \"evaluatedThrough\": \"2026-07-14\", \"completionDate\": null, \"idleDays\": 0, \"seasonState\": \"IN_PROGRESS\", \"daysAbove24Celsius\": 2, \"currentWarmStreakDays\": 0, \"longestWarmStreakDays\": 2, \"thermalAnomaly\": \"NONE\", \"projectionStatus\": \"PROJECTED\", \"projectedCompletionDate\": \"2026-08-09\", \"previousSeason\": {\"seasonYear\": 2025, \"accumulatedPortions\": 27.2, \"completionDate\": null, \"daysAbove24Celsius\": 1}, \"dailyCurve\": [{\"date\": \"2026-06-01\", \"portions\": 0.0, \"previousSeasonPortions\": 0.0}]}, \"evaluatedAt\": \"2026-07-15T14:00:00Z\"}"
)
@NullMarked
public record MetricResource(
        @Schema(description = "Evaluated metric type name", example = "EREZ_CHILLING_PORTIONS")
        String metricName,

        @Schema(description = "Quantitative numerical value of the metric", example = "28.5")
        Double value,

        @Schema(description = "Qualitative agronomic classification or status. For BIENNIAL_BEARING_INDEX: REGULAR (< 0.20), MODERATE_ALTERNATION (0.20-0.40), SEVERE_ALTERNATION (> 0.40), INSUFFICIENT_DATA (fewer than 2 campaigns)", example = "SATISFIED")
        String qualitativeCategory,

        @Schema(description = "Detailed parameters and formula metadata. For EREZ_CHILLING_PORTIONS: thresholdTarget, completionPercentage, "
                + "seasonYear/seasonStart/seasonEnd (June 1 - August 31; outside it, the last finished winter), evaluatedThrough, "
                + "completionDate, idleDays, seasonState (IN_PROGRESS, HALTED, COMPLETED, OFF_SEASON), daysAbove24Celsius, "
                + "currentWarmStreakDays, longestWarmStreakDays, thermalAnomaly (NONE, ACTIVE, RECORDED: more than 3 days in a row "
                + "above 24 °C), projectionStatus (PROJECTED, NOT_REACHABLE_IN_SEASON, INSUFFICIENT_DATA, NOT_APPLICABLE), "
                + "projectedCompletionDate (pace of the last 14 days), previousSeason {seasonYear, accumulatedPortions, completionDate, "
                + "daysAbove24Celsius} or null, and dailyCurve with one item per season day {date, portions, previousSeasonPortions}, "
                + "null where there is no data")
        Map<String, Object> details,

        @Schema(description = "Evaluation timestamp", example = "2026-09-28T14:00:00Z")
        Instant evaluatedAt
) {
}

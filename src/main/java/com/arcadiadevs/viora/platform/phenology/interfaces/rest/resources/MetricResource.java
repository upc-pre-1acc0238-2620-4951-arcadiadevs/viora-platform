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
        example = "{\"metricName\": \"EREZ_CHILLING_PORTIONS\", \"value\": 28.5, \"qualitativeCategory\": \"SATISFIED\", \"details\": {\"model\": \"Dynamic Erez-Fishman\", \"thresholdTarget\": 27.0, \"completionPercentage\": 105.56, \"seasonStart\": \"2026-06-01\", \"completionDate\": \"2026-08-18\", \"idleDays\": 0, \"seasonState\": \"COMPLETED\"}, \"evaluatedAt\": \"2026-09-28T14:00:00Z\"}"
)
@NullMarked
public record MetricResource(
        @Schema(description = "Evaluated metric type name", example = "EREZ_CHILLING_PORTIONS")
        String metricName,

        @Schema(description = "Quantitative numerical value of the metric", example = "28.5")
        Double value,

        @Schema(description = "Qualitative agronomic classification or status. For BIENNIAL_BEARING_INDEX: REGULAR (< 0.20), MODERATE_ALTERNATION (0.20-0.40), SEVERE_ALTERNATION (> 0.40), INSUFFICIENT_DATA (fewer than 2 campaigns)", example = "SATISFIED")
        String qualitativeCategory,

        @Schema(description = "Detailed parameters and formula metadata (includes seasonStart, completionDate, idleDays, and seasonState for chilling metrics)")
        Map<String, Object> details,

        @Schema(description = "Evaluation timestamp", example = "2026-09-28T14:00:00Z")
        Instant evaluatedAt
) {
}

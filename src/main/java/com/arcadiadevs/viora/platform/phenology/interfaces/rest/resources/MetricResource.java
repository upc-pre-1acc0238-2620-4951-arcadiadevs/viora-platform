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
        description = "Response resource representing an evaluated phenological metric",
        example = "{\"metricName\": \"BIENNIAL_BEARING_INDEX\", \"value\": 0.42, \"qualitativeCategory\": \"MODERATE_ALTERNATION\", \"details\": {\"formula\": \"Hoblyn (1936)\", \"evaluatedYearsCount\": 4, \"sampleSufficiency\": \"SUFFICIENT\"}, \"evaluatedAt\": \"2026-09-28T14:00:00Z\"}"
)
@NullMarked
public record MetricResource(
        @Schema(description = "Evaluated metric type name", example = "BIENNIAL_BEARING_INDEX")
        String metricName,

        @Schema(description = "Quantitative numerical value of the metric", example = "0.42")
        Double value,

        @Schema(description = "Qualitative agronomic classification or status", example = "MODERATE_ALTERNATION")
        String qualitativeCategory,

        @Schema(description = "Detailed parameters and formula metadata")
        Map<String, Object> details,

        @Schema(description = "Evaluation timestamp", example = "2026-09-28T14:00:00Z")
        Instant evaluatedAt
) {
}

package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Domain evaluation result capturing the calculated metrics for an olive orchard plot.
 *
 * @param metricType          the specific phenological metric
 * @param value               the computed numerical value
 * @param qualitativeCategory the qualitative agronomic category or threshold status
 * @param details             detailed formula metadata, counts, or parameters
 * @param evaluatedAt         the timestamp of calculation
 */
public record MetricEvaluationResult(
        MetricType metricType,
        Double value,
        String qualitativeCategory,
        Map<String, Object> details,
        Instant evaluatedAt
) {
    /**
     * Compact constructor validating non-null requirements.
     */
    public MetricEvaluationResult {
        if (metricType == null) {
            throw new IllegalArgumentException("phenology.metric_type.null");
        }
        if (value == null) {
            throw new IllegalArgumentException("phenology.metric_value.null");
        }
        if (qualitativeCategory == null || qualitativeCategory.isBlank()) {
            throw new IllegalArgumentException("phenology.qualitative_category.null_or_empty");
        }
        if (details == null) {
            details = Map.of();
        } else {
            details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
        }
        if (evaluatedAt == null) {
            evaluatedAt = Instant.now();
        }
    }
}

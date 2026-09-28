package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

import java.util.Locale;

/**
 * Metric catalog identifying agronomic and biological phenological indices.
 */
public enum MetricType {
    BIENNIAL_BEARING_INDEX,
    EREZ_CHILLING_PORTIONS,
    GROWING_DEGREE_DAYS;

    /**
     * Parses a string input into a {@link MetricType}, recognizing standard names and common abbreviations.
     *
     * @param value the raw string value (e.g., "BBI", "CHILLING", "BIENNIAL_BEARING_INDEX")
     * @return the matching {@link MetricType}
     * @throws IllegalArgumentException if the provided name does not match any recognized metric
     */
    public static MetricType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("phenology.metric_type.null_or_empty");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BBI", "BIENNIAL_BEARING_INDEX", "BEARING_INDEX" -> BIENNIAL_BEARING_INDEX;
            case "CHILLING", "EREZ", "EREZ_CHILLING_PORTIONS", "CHILL_PORTIONS" -> EREZ_CHILLING_PORTIONS;
            case "GDD", "GROWING_DEGREE_DAYS", "THERMAL_SUM" -> GROWING_DEGREE_DAYS;
            default -> throw new IllegalArgumentException("phenology.metric_type.invalid");
        };
    }
}

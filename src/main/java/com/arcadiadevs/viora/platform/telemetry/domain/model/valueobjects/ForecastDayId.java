package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the unique identifier of a weather forecast day record.
 * Encapsulates a valid UUID string and enforces domain identity invariants.
 *
 * @param forecastDayId the unique forecast day identifier UUID string
 */
public record ForecastDayId(String forecastDayId) {

    /**
     * Default constructor generating a new random UUID.
     */
    public ForecastDayId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor validating that the identifier is a non-blank, valid UUID string.
     *
     * @throws IllegalArgumentException if the forecastDayId is null, blank, or malformed
     */
    public ForecastDayId {
        if (forecastDayId == null || forecastDayId.isBlank()) {
            throw new IllegalArgumentException("telemetry.forecast_day.id.null_or_empty");
        }
        try {
            forecastDayId = UUID.fromString(forecastDayId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("telemetry.forecast_day.id.invalid_uuid", exception);
        }
    }
}

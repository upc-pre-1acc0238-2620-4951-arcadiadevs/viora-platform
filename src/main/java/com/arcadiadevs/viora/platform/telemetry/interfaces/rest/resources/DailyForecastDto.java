package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Public DTO representing an individual day's meteorological forecast.
 *
 * @param forecastDate             projected calendar date
 * @param maxTemperature           maximum predicted temperature in °C
 * @param minTemperature           minimum predicted temperature in °C
 * @param precipitationProbability precipitation probability in % (0.0 to 100.0)
 * @param windSpeedKmh             wind speed in km/h
 * @param isFrostRisk              true if minimum temperature triggers frost warning (< 2.0 °C)
 * @param syncedAt                 timestamp of meteorological sync
 */
@Schema(description = "Individual daily agroclimatic weather forecast projection")
public record DailyForecastDto(
        @Schema(description = "Projected calendar date", example = "2026-09-29")
        LocalDate forecastDate,

        @Schema(description = "Maximum predicted temperature in °C", example = "24.5")
        Double maxTemperature,

        @Schema(description = "Minimum predicted temperature in °C", example = "8.2")
        Double minTemperature,

        @Schema(description = "Precipitation probability in % (0.0 - 100.0)", example = "15.0")
        Double precipitationProbability,

        @Schema(description = "Wind speed in km/h", example = "14.0")
        Double windSpeedKmh,

        @Schema(description = "True if minimum temperature drops below 2.0 °C (frost warning)", example = "false")
        Boolean isFrostRisk,

        @Schema(description = "Timestamp of meteorological synchronization", example = "2026-09-28T22:00:00Z")
        Instant syncedAt
) {}

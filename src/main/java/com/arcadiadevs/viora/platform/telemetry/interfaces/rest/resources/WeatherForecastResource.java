package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Public REST resource representing the 7-day agroclimatic weather forecast for an orchard plot.
 *
 * @param plotId         unique plot identifier (UUID)
 * @param dailyForecasts ordered list of 7 daily meteorological forecasts
 * @param generatedAt    timestamp when the forecast response was generated
 */
@Schema(description = "7-day agroclimatic weather forecast bundle for an olive orchard plot")
public record WeatherForecastResource(
        @Schema(description = "Unique plot identifier UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String plotId,

        @Schema(description = "List of 7 daily forecast projections")
        List<DailyForecastDto> dailyForecasts,

        @Schema(description = "Timestamp when forecast was computed/retrieved", example = "2026-09-28T23:00:00Z")
        Instant generatedAt
) {}

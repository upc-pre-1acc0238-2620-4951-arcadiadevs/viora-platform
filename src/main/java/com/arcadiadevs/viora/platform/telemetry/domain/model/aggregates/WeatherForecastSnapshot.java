package com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;

import java.time.Instant;
import java.util.List;

/**
 * Immutable domain snapshot representing the aggregated 7-day agroclimatic forecast for an orchard plot.
 *
 * @param plotId         the referenced orchard plot identifier
 * @param dailyForecasts ordered list of 7 daily forecast snapshots
 * @param generatedAt    timestamp when the forecast bundle was computed/synchronized
 */
public record WeatherForecastSnapshot(
        PlotId plotId,
        List<WeatherForecastDaySnapshot> dailyForecasts,
        Instant generatedAt
) {
    public WeatherForecastSnapshot {
        if (plotId == null) {
            throw new IllegalArgumentException("device.plot_id.null_or_empty");
        }
        if (dailyForecasts == null) {
            throw new IllegalArgumentException("telemetry.forecast.list.null");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("telemetry.reading_timestamp.null");
        }
        dailyForecasts = List.copyOf(dailyForecasts);
    }
}

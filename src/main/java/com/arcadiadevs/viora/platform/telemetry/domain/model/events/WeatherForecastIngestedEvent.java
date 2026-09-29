package com.arcadiadevs.viora.platform.telemetry.domain.model.events;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Domain event dispatched when weather forecasts are synchronized with Open-Meteo or meteorological providers.
 *
 * @param plotId       the referenced plot identifier string
 * @param forecastDate the first projected forecast date
 * @param minTemp      the lowest projected temperature in the series
 * @param occurredOn   the event occurrence timestamp
 */
public record WeatherForecastIngestedEvent(
        String plotId,
        LocalDate forecastDate,
        Double minTemp,
        Instant occurredOn
) {
    public WeatherForecastIngestedEvent {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("device.plot_id.null_or_empty");
        }
        if (forecastDate == null) {
            throw new IllegalArgumentException("telemetry.forecast_day.date.null");
        }
        if (minTemp == null) {
            throw new IllegalArgumentException("telemetry.temperature.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("telemetry.reading_timestamp.null");
        }
    }
}

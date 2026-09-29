package com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.AmbientTemperature;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.ForecastDayId;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.Percentage;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.WindSpeed;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Immutable domain snapshot representing a daily meteorological forecast projection.
 *
 * @param id                       the unique forecast day identifier
 * @param forecastDate             projected calendar date
 * @param maxTemperature           predicted maximum temperature in °C
 * @param minTemperature           predicted minimum temperature in °C
 * @param precipitationProbability predicted precipitation probability in %
 * @param windSpeedKmh             predicted wind speed in km/h
 * @param isFrostRisk              true if minimum temperature is below 2.0 °C
 * @param syncedAt                 timestamp of meteorological data synchronization
 */
public record WeatherForecastDaySnapshot(
        ForecastDayId id,
        LocalDate forecastDate,
        AmbientTemperature maxTemperature,
        AmbientTemperature minTemperature,
        Percentage precipitationProbability,
        WindSpeed windSpeedKmh,
        boolean isFrostRisk,
        Instant syncedAt
) {
    public WeatherForecastDaySnapshot {
        if (id == null) {
            throw new IllegalArgumentException("telemetry.forecast_day.id.null_or_empty");
        }
        if (forecastDate == null) {
            throw new IllegalArgumentException("telemetry.forecast_day.date.null");
        }
        if (maxTemperature == null || minTemperature == null) {
            throw new IllegalArgumentException("telemetry.temperature.null");
        }
        if (precipitationProbability == null) {
            throw new IllegalArgumentException("telemetry.percentage.null");
        }
        if (windSpeedKmh == null) {
            throw new IllegalArgumentException("telemetry.wind_speed.null");
        }
        if (syncedAt == null) {
            throw new IllegalArgumentException("telemetry.reading_timestamp.null");
        }
    }
}

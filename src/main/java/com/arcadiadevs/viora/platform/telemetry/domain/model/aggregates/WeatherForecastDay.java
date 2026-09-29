package com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.AmbientTemperature;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.ForecastDayId;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.Percentage;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.WindSpeed;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Pure domain entity representing a single day's agroclimatic forecast.
 *
 * <p>Enforces physiological thresholds for olive groves, notably identifying
 * radiative and advective frost risk whenever minimum predicted temperatures fall below 2.0 °C.</p>
 */
public class WeatherForecastDay {

    public static final double FROST_RISK_THRESHOLD_CELSIUS = 2.0;

    private final ForecastDayId id;
    private final LocalDate forecastDate;
    private final AmbientTemperature maxTemperature;
    private final AmbientTemperature minTemperature;
    private final Percentage precipitationProbability;
    private final WindSpeed windSpeedKmh;
    private final Instant syncedAt;

    private WeatherForecastDay(
            ForecastDayId id,
            LocalDate forecastDate,
            AmbientTemperature maxTemperature,
            AmbientTemperature minTemperature,
            Percentage precipitationProbability,
            WindSpeed windSpeedKmh,
            Instant syncedAt
    ) {
        this.id = id;
        this.forecastDate = forecastDate;
        this.maxTemperature = maxTemperature;
        this.minTemperature = minTemperature;
        this.precipitationProbability = precipitationProbability;
        this.windSpeedKmh = windSpeedKmh;
        this.syncedAt = syncedAt;
    }

    /**
     * Factory method creating a new domain forecast day instance.
     *
     * @param forecastDate             projected calendar date
     * @param maxTemperature           predicted maximum temperature in °C
     * @param minTemperature           predicted minimum temperature in °C
     * @param precipitationProbability predicted precipitation probability in %
     * @param windSpeedKmh             predicted wind speed in km/h
     * @param syncedAt                 timestamp of meteorological sync
     * @return a valid {@link WeatherForecastDay} instance
     */
    public static WeatherForecastDay create(
            LocalDate forecastDate,
            AmbientTemperature maxTemperature,
            AmbientTemperature minTemperature,
            Percentage precipitationProbability,
            WindSpeed windSpeedKmh,
            Instant syncedAt
    ) {
        if (forecastDate == null) {
            throw new IllegalArgumentException("telemetry.forecast_day.date.null");
        }
        if (maxTemperature == null || minTemperature == null) {
            throw new IllegalArgumentException("telemetry.temperature.null");
        }
        if (maxTemperature.celsius() < minTemperature.celsius()) {
            throw new IllegalArgumentException("telemetry.forecast.temperature_inversion");
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

        return new WeatherForecastDay(
                new ForecastDayId(),
                forecastDate,
                maxTemperature,
                minTemperature,
                precipitationProbability,
                windSpeedKmh,
                syncedAt
        );
    }

    /**
     * Reconstitutes an entity instance from an immutable persistence or DTO snapshot.
     *
     * @param snapshot the snapshot to reconstitute from
     * @return reconstituted {@link WeatherForecastDay} instance
     */
    public static WeatherForecastDay reconstitute(WeatherForecastDaySnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("telemetry.forecast.snapshot.null");
        }
        if (snapshot.maxTemperature().celsius() < snapshot.minTemperature().celsius()) {
            throw new IllegalArgumentException("telemetry.forecast.temperature_inversion");
        }
        return new WeatherForecastDay(
                snapshot.id(),
                snapshot.forecastDate(),
                snapshot.maxTemperature(),
                snapshot.minTemperature(),
                snapshot.precipitationProbability(),
                snapshot.windSpeedKmh(),
                snapshot.syncedAt()
        );
    }

    /**
     * Detects if the projected minimum temperature triggers an olive frost risk warning (&lt; 2.0 °C).
     *
     * @return {@code true} if minTemperature &lt; 2.0 °C; {@code false} otherwise
     */
    public boolean isFrostRisk() {
        return minTemperature.celsius() < FROST_RISK_THRESHOLD_CELSIUS;
    }

    /**
     * Exports an immutable snapshot of this forecast day entity.
     *
     * @return an immutable {@link WeatherForecastDaySnapshot}
     */
    public WeatherForecastDaySnapshot snapshot() {
        return new WeatherForecastDaySnapshot(
                id,
                forecastDate,
                maxTemperature,
                minTemperature,
                precipitationProbability,
                windSpeedKmh,
                isFrostRisk(),
                syncedAt
        );
    }

    public ForecastDayId id() {
        return id;
    }

    public LocalDate forecastDate() {
        return forecastDate;
    }

    public AmbientTemperature maxTemperature() {
        return maxTemperature;
    }

    public AmbientTemperature minTemperature() {
        return minTemperature;
    }

    public Percentage precipitationProbability() {
        return precipitationProbability;
    }

    public WindSpeed windSpeedKmh() {
        return windSpeedKmh;
    }

    public Instant syncedAt() {
        return syncedAt;
    }
}

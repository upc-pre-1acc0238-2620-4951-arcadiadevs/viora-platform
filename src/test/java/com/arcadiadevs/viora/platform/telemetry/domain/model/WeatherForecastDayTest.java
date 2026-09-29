package com.arcadiadevs.viora.platform.telemetry.domain.model;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDay;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.AmbientTemperature;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.ForecastDayId;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.Percentage;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.WindSpeed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WeatherForecastDay Domain Entity and Value Objects Tests")
class WeatherForecastDayTest {

    @Test
    @DisplayName("Should create valid WeatherForecastDay and accurately evaluate frost risk")
    void shouldCreateValidWeatherForecastDayAndEvaluateFrostRisk() {
        var date = LocalDate.of(2026, 9, 29);
        var maxTemp = new AmbientTemperature(18.5);
        var minTemp = new AmbientTemperature(1.5); // < 2.0 -> frost risk
        var precip = new Percentage(20.0);
        var wind = new WindSpeed(15.0);
        var syncedAt = Instant.now();

        var day = WeatherForecastDay.create(date, maxTemp, minTemp, precip, wind, syncedAt);

        assertThat(day.id()).isNotNull();
        assertThat(day.forecastDate()).isEqualTo(date);
        assertThat(day.maxTemperature().celsius()).isEqualTo(18.5);
        assertThat(day.minTemperature().celsius()).isEqualTo(1.5);
        assertThat(day.precipitationProbability().value()).isEqualTo(20.0);
        assertThat(day.windSpeedKmh().kmh()).isEqualTo(15.0);
        assertThat(day.isFrostRisk()).isTrue();

        var snapshot = day.snapshot();
        assertThat(snapshot.isFrostRisk()).isTrue();

        var reconstituted = WeatherForecastDay.reconstitute(snapshot);
        assertThat(reconstituted.id()).isEqualTo(day.id());
        assertThat(reconstituted.isFrostRisk()).isTrue();
    }

    @Test
    @DisplayName("Should return false for isFrostRisk when minimum temperature is 2.0 C or higher")
    void shouldReturnFalseForFrostRiskWhenTemperatureAboveThreshold() {
        var date = LocalDate.of(2026, 9, 30);
        var maxTemp = new AmbientTemperature(24.0);
        var minTemp = new AmbientTemperature(2.0); // exactly 2.0 -> not frost risk
        var precip = new Percentage(0.0);
        var wind = new WindSpeed(10.0);
        var syncedAt = Instant.now();

        var day = WeatherForecastDay.create(date, maxTemp, minTemp, precip, wind, syncedAt);

        assertThat(day.isFrostRisk()).isFalse();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when temperature inversion occurs (max < min)")
    void shouldThrowWhenTemperatureInversionOccurs() {
        var date = LocalDate.of(2026, 9, 30);
        var maxTemp = new AmbientTemperature(5.0);
        var minTemp = new AmbientTemperature(15.0);
        var precip = new Percentage(10.0);
        var wind = new WindSpeed(10.0);
        var syncedAt = Instant.now();

        assertThatThrownBy(() -> WeatherForecastDay.create(date, maxTemp, minTemp, precip, wind, syncedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.forecast.temperature_inversion");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on null fields")
    void shouldThrowOnNullFields() {
        var date = LocalDate.now();
        var temp = new AmbientTemperature(15.0);
        var precip = new Percentage(10.0);
        var wind = new WindSpeed(10.0);
        var syncedAt = Instant.now();

        assertThatThrownBy(() -> WeatherForecastDay.create(null, temp, temp, precip, wind, syncedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.forecast_day.date.null");

        assertThatThrownBy(() -> WeatherForecastDay.create(date, null, temp, precip, wind, syncedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.temperature.null");

        assertThatThrownBy(() -> WeatherForecastDay.create(date, temp, temp, null, wind, syncedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.percentage.null");

        assertThatThrownBy(() -> WeatherForecastDay.create(date, temp, temp, precip, null, syncedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.wind_speed.null");

        assertThatThrownBy(() -> WeatherForecastDay.create(date, temp, temp, precip, wind, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.reading_timestamp.null");
    }

    @Test
    @DisplayName("Should validate Percentage and WindSpeed value object invariants")
    void shouldValidateValueObjectInvariants() {
        assertThatThrownBy(() -> new Percentage(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.percentage.null");

        assertThatThrownBy(() -> new Percentage(-0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.percentage.out_of_range");

        assertThatThrownBy(() -> new Percentage(100.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.percentage.out_of_range");

        assertThatThrownBy(() -> new WindSpeed(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.wind_speed.null");

        assertThatThrownBy(() -> new WindSpeed(-1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.wind_speed.out_of_range");

        assertThatThrownBy(() -> new WindSpeed(350.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.wind_speed.out_of_range");

        assertThatThrownBy(() -> new ForecastDayId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.forecast_day.id.null_or_empty");

        assertThatThrownBy(() -> new ForecastDayId("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.forecast_day.id.invalid_uuid");
    }
}

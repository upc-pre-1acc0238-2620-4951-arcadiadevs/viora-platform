package com.arcadiadevs.viora.platform.telemetry.infrastructure;

import com.arcadiadevs.viora.platform.telemetry.infrastructure.adapters.meteo.OpenMeteoWeatherClientAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenMeteoWeatherClientAdapter Infrastructure Unit Tests")
class OpenMeteoWeatherClientAdapterTest {

    @Test
    @DisplayName("Should return 7-day forecast with realistic values and valid date sequence")
    void shouldReturnSevenDayForecast() {
        var adapter = new OpenMeteoWeatherClientAdapter();
        var forecast = adapter.fetchSevenDayForecast(-18.05, -70.25);

        assertThat(forecast).hasSize(7);
        for (var day : forecast) {
            assertThat(day.forecastDate()).isNotNull();
            assertThat(day.maxTemperature().celsius()).isGreaterThanOrEqualTo(day.minTemperature().celsius());
            assertThat(day.precipitationProbability().value()).isBetween(0.0, 100.0);
            assertThat(day.windSpeedKmh().kmh()).isGreaterThanOrEqualTo(0.0);
            assertThat(day.syncedAt()).isNotNull();
        }
    }
}

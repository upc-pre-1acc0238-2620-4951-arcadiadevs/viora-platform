package com.arcadiadevs.viora.platform.telemetry.infrastructure.adapters.meteo;

import com.arcadiadevs.viora.platform.telemetry.infrastructure.adapters.meteo.OpenMeteoHourlyWeatherAdapter.HourlyData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

@DisplayName("OpenMeteoHourlyWeatherAdapter Unit Tests")
class OpenMeteoHourlyWeatherAdapterTest {

    @Test
    @DisplayName("Should read the past hours in UTC, drop the hours to come and turn m³/m³ into %")
    void shouldParseThePastHours() {
        var hourly = new HourlyData(
                List.of("2026-10-07T13:00", "2026-10-07T14:00", "2026-10-07T15:00", "2026-10-07T16:00"),
                List.of(19.5, 22.0, 24.1, 25.3),
                List.of(70.0, 58.0, 50.0, 45.0),
                List.of(0.206, 0.206, 0.207, 0.207),
                List.of(480.0, 706.0, 850.0, 928.0)
        );

        var observations = OpenMeteoHourlyWeatherAdapter.parse(hourly, Instant.parse("2026-10-07T15:20:00Z"));

        assertThat(observations).hasSize(3);
        assertThat(observations.get(0).hour()).isEqualTo(Instant.parse("2026-10-07T13:00:00Z"));
        assertThat(observations.get(2).hour()).isEqualTo(Instant.parse("2026-10-07T15:00:00Z"));
        assertThat(observations.get(1).temperatureCelsius()).isEqualTo(22.0);
        assertThat(observations.get(1).relativeHumidity()).isEqualTo(58.0);
        assertThat(observations.get(1).soilMoisturePercent()).isCloseTo(20.6, offset(1e-9));
        assertThat(observations.get(1).solarRadiation()).isEqualTo(706.0);
    }

    @Test
    @DisplayName("Should skip the hours with a missing value instead of inventing one")
    void shouldSkipIncompleteHours() {
        var hourly = new HourlyData(
                List.of("2026-10-07T10:00", "2026-10-07T11:00"),
                List.of(16.0, 17.0),
                List.of(90.0, 88.0),
                Arrays.asList(null, 0.206),
                List.of(0.0, 95.0)
        );

        var observations = OpenMeteoHourlyWeatherAdapter.parse(hourly, Instant.parse("2026-10-07T15:00:00Z"));

        assertThat(observations).hasSize(1);
        assertThat(observations.get(0).hour()).isEqualTo(Instant.parse("2026-10-07T11:00:00Z"));
    }
}

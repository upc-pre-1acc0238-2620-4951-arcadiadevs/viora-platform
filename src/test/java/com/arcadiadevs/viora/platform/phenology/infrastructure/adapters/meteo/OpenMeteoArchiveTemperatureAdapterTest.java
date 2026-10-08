package com.arcadiadevs.viora.platform.phenology.infrastructure.adapters.meteo;

import com.arcadiadevs.viora.platform.phenology.infrastructure.adapters.meteo.OpenMeteoArchiveTemperatureAdapter.HourlyData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenMeteoArchiveTemperatureAdapter Unit Tests")
class OpenMeteoArchiveTemperatureAdapterTest {

    @Test
    @DisplayName("Should read the hourly temperatures in the plot's local time")
    void shouldParseTheHours() {
        var hourly = new HourlyData(
                List.of("2026-06-01T00:00", "2026-06-01T01:00", "2026-06-01T02:00"),
                List.of(11.2, 10.8, 10.5)
        );

        var hours = OpenMeteoArchiveTemperatureAdapter.parse(hourly);

        assertThat(hours).hasSize(3);
        assertThat(hours.getFirst().hour()).isEqualTo(LocalDateTime.of(2026, 6, 1, 0, 0));
        assertThat(hours.get(2).celsius()).isEqualTo(10.5);
    }

    @Test
    @DisplayName("Should skip the hours without a value instead of inventing one")
    void shouldSkipMissingHours() {
        var hourly = new HourlyData(
                List.of("2026-10-07T00:00", "2026-10-07T01:00", "2026-10-07T02:00"),
                Arrays.asList(15.0, null, null)
        );

        var hours = OpenMeteoArchiveTemperatureAdapter.parse(hourly);

        assertThat(hours).hasSize(1);
        assertThat(hours.getFirst().hour()).isEqualTo(LocalDateTime.of(2026, 10, 7, 0, 0));
    }
}

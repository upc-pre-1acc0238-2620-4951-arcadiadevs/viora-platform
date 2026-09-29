package com.arcadiadevs.viora.platform.telemetry.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.WeatherForecastQueryService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDaySnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetWeatherForecastByPlotIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.controllers.WeatherForecastController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherForecastController REST Endpoint Integration Tests")
class WeatherForecastControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private WeatherForecastQueryService weatherForecastQueryService;

    private final UUID plotId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @BeforeEach
    void setUp() {
        var controller = new WeatherForecastController(weatherForecastQueryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/forecasts should return 200 with 7-day forecast bundle")
    void shouldReturnWeatherForecastSuccessfully() throws Exception {
        var day = new WeatherForecastDaySnapshot(
                new ForecastDayId(),
                LocalDate.of(2026, 9, 29),
                new AmbientTemperature(23.5),
                new AmbientTemperature(1.8),
                new Percentage(15.0),
                new WindSpeed(12.0),
                true,
                Instant.parse("2026-09-28T22:00:00Z")
        );

        var snapshot = new WeatherForecastSnapshot(
                new PlotId(plotId.toString()),
                List.of(day),
                Instant.parse("2026-09-28T22:30:00Z")
        );

        when(weatherForecastQueryService.handle(any(GetWeatherForecastByPlotIdQuery.class)))
                .thenReturn(Result.success(snapshot));

        mockMvc.perform(get("/api/v1/plots/{plotId}/forecasts", plotId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plotId", is(plotId.toString())))
                .andExpect(jsonPath("$.dailyForecasts", hasSize(1)))
                .andExpect(jsonPath("$.dailyForecasts[0].forecastDate", is("2026-09-29")))
                .andExpect(jsonPath("$.dailyForecasts[0].maxTemperature", is(23.5)))
                .andExpect(jsonPath("$.dailyForecasts[0].minTemperature", is(1.8)))
                .andExpect(jsonPath("$.dailyForecasts[0].precipitationProbability", is(15.0)))
                .andExpect(jsonPath("$.dailyForecasts[0].windSpeedKmh", is(12.0)))
                .andExpect(jsonPath("$.dailyForecasts[0].isFrostRisk", is(true)))
                .andExpect(jsonPath("$.generatedAt", is("2026-09-28T22:30:00Z")));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/forecasts should return 404 when plot is not found")
    void shouldReturn404WhenPlotNotFound() throws Exception {
        when(weatherForecastQueryService.handle(any(GetWeatherForecastByPlotIdQuery.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("Plot", plotId.toString())));

        mockMvc.perform(get("/api/v1/plots/{plotId}/forecasts", plotId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Not Found")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/forecasts should return 400 when plot UUID is malformed")
    void shouldReturn400WhenPlotIdIsMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/plots/{plotId}/forecasts", "invalid-uuid-format")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}

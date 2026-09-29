package com.arcadiadevs.viora.platform.telemetry.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.TelemetryQueryService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetTelemetrySeriesByPlotIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.controllers.TelemetryController;
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
@DisplayName("TelemetryController REST Endpoint Integration Tests")
class TelemetryControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private TelemetryQueryService telemetryQueryService;

    private final UUID plotId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @BeforeEach
    void setUp() {
        var controller = new TelemetryController(telemetryQueryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/telemetries should return 200 with list of observations")
    void shouldReturnTelemetryObservationsSuccessfully() throws Exception {
        var reading = new HourlyTelemetryReadingSnapshot(
                new HourlyReadingId(),
                new ReadingTimestamp(Instant.parse("2026-09-28T10:00:00Z")),
                new AmbientTemperature(21.5),
                new RelativeHumidity(54.0),
                new SoilMoisture(26.8),
                new SolarRadiation(620.0),
                new StemWaterPotential(-1.1)
        );

        when(telemetryQueryService.handle(any(GetTelemetrySeriesByPlotIdQuery.class)))
                .thenReturn(Result.success(List.of(reading)));

        mockMvc.perform(get("/api/v1/plots/{plotId}/telemetries", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].plotId", is(plotId.toString())))
                .andExpect(jsonPath("$[0].temperature", is(21.5)))
                .andExpect(jsonPath("$[0].humidity", is(54.0)))
                .andExpect(jsonPath("$[0].soilMoisture", is(26.8)))
                .andExpect(jsonPath("$[0].solarRadiation", is(620.0)))
                .andExpect(jsonPath("$[0].stemWaterPotential", is(-1.1)))
                .andExpect(jsonPath("$[0].recordedAt", is("2026-09-28T10:00:00Z")));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/telemetries with date filters should return 200")
    void shouldReturnFilteredObservationsWithDateRange() throws Exception {
        when(telemetryQueryService.handle(any(GetTelemetrySeriesByPlotIdQuery.class)))
                .thenReturn(Result.success(List.of()));

        mockMvc.perform(get("/api/v1/plots/{plotId}/telemetries", plotId)
                        .param("startDate", "2026-09-01T00:00:00Z")
                        .param("endDate", "2026-09-02T00:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/telemetries should return 404 when plot is not found")
    void shouldReturnNotFoundWhenPlotDoesNotExist() throws Exception {
        when(telemetryQueryService.handle(any(GetTelemetrySeriesByPlotIdQuery.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("Plot", plotId.toString())));

        mockMvc.perform(get("/api/v1/plots/{plotId}/telemetries", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Not Found")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/telemetries with invalid plot UUID should return 400")
    void shouldReturnBadRequestWhenPlotUuidIsMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/plots/{plotId}/telemetries", "not-a-valid-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}

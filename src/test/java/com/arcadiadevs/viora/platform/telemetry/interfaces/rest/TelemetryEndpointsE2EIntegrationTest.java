package com.arcadiadevs.viora.platform.telemetry.interfaces.rest;

import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider.HourlyWeatherObservation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests verifying telemetry REST endpoints against the live Spring Boot container and database.
 */
@SpringBootTest
@DisplayName("Telemetry REST Endpoints Live E2E Integration Tests")
class TelemetryEndpointsE2EIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    /** Open-Meteo stays out of the tests: the virtual node gets the last three completed hours. */
    @MockitoBean
    private HourlyWeatherProvider hourlyWeatherProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private String plotId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        var currentHour = Instant.now().truncatedTo(ChronoUnit.HOURS);
        when(hourlyWeatherProvider.fetchPastHours(anyDouble(), anyDouble(), anyInt())).thenReturn(List.of(
                new HourlyWeatherObservation(currentHour.minus(Duration.ofHours(3)), 16.5, 84.0, 20.6, 0.0),
                new HourlyWeatherObservation(currentHour.minus(Duration.ofHours(2)), 18.0, 78.0, 20.6, 95.0),
                new HourlyWeatherObservation(currentHour.minus(Duration.ofHours(1)), 21.5, 63.0, 20.7, 476.0)
        ));

        // 1. Create a persistent plot in the live DB to instrument
        String plotPayload = """
                {
                  "producerId": "%s",
                  "name": "Cuartel Telemétrico %s",
                  "variety": "ARBEQUINA",
                  "polygonGeoJson": "{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                  "rowSpacingM": 6.0,
                  "treeSpacingM": 4.0
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID().toString().substring(0, 8));

        var plotResponse = mockMvc.perform(post("/api/v1/plots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(plotPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode plotJson = objectMapper.readTree(plotResponse);
        this.plotId = plotJson.get("id").asText();
    }

    @Test
    @DisplayName("Should execute complete lifecycle: POST register -> GET list -> PUT calibrate")
    void shouldExecuteCompleteTelemetryLifecycle() throws Exception {
        // 1. POST: Register virtual soil probe
        String registerPayload = """
                {
                    "name": "Sonda Edafica Sector Centro",
                    "type": "SOIL_PROBE",
                    "depthCm": 30,
                    "soilTextureType": "SANDY_LOAM",
                    "calibrationMultiplier": 1.05
                }
                """;

        var registerResult = mockMvc.perform(post("/api/v1/plots/{plotId}/iot-devices", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.plotId", is(plotId)))
                .andExpect(jsonPath("$.name", is("Sonda Edafica Sector Centro")))
                .andExpect(jsonPath("$.type", is("SOIL_PROBE")))
                .andExpect(jsonPath("$.depthCm", is(30)))
                .andExpect(jsonPath("$.soilTextureType", is("SANDY_LOAM")))
                .andExpect(jsonPath("$.calibrationMultiplier", is(1.05)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.revision", is(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode deviceJson = objectMapper.readTree(registerResult);
        String deviceId = deviceJson.get("id").asText();

        // 2. GET: List devices bound to plot
        mockMvc.perform(get("/api/v1/plots/{plotId}/iot-devices", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(deviceId)))
                .andExpect(jsonPath("$[0].name", is("Sonda Edafica Sector Centro")));

        // 3. PUT: Calibrate device with If-Match: "0"
        String calibratePayload = """
                {
                    "calibrationMultiplier": 1.25,
                    "soilTextureType": "CLAY_LOAM",
                    "depthCm": 45
                }
                """;

        mockMvc.perform(put("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"0\"")
                        .content(calibratePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(deviceId)))
                .andExpect(jsonPath("$.plotId", is(plotId)))
                .andExpect(jsonPath("$.calibrationMultiplier", is(1.25)))
                .andExpect(jsonPath("$.soilTextureType", is("CLAY_LOAM")))
                .andExpect(jsonPath("$.depthCm", is(45)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.revision", is(1)));

        // 4. PUT: Calibrate with stale revision "0" should fail with 412 Precondition Failed
        mockMvc.perform(put("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"0\"")
                        .content(calibratePayload))
                .andExpect(status().isPreconditionFailed());

        // 5. POST: Attempt duplicate name in same plot should fail with 409 Conflict
        mockMvc.perform(post("/api/v1/plots/{plotId}/iot-devices", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isConflict());

        // 6. POST: Invalid payload with out-of-range multiplier should fail with 400 Bad Request
        String invalidPayload = """
                {
                    "name": "Sonda Invalida",
                    "type": "SOIL_PROBE",
                    "depthCm": 30,
                    "calibrationMultiplier": 5.00
                }
                """;
        mockMvc.perform(post("/api/v1/plots/{plotId}/iot-devices", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());

        // 7. DELETE: Unlink device with If-Match: "1"
        mockMvc.perform(delete("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId)
                        .header("If-Match", "\"1\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("IoT device unlinked successfully")));

        // 8. GET: Check device list reflects UNLINKED status and incremented revision
        mockMvc.perform(get("/api/v1/plots/{plotId}/iot-devices", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(deviceId)))
                .andExpect(jsonPath("$[0].status", is("UNLINKED")))
                .andExpect(jsonPath("$[0].revision", is(2)));

        // 9. DELETE: Unlink already unlinked device should fail with 409 Conflict
        mockMvc.perform(delete("/api/v1/plots/{plotId}/iot-devices/{deviceId}", plotId, deviceId))
                .andExpect(status().isConflict());

        // 10. GET: Query telemetry series for the active plot (the virtual node fills the observed hours)
        mockMvc.perform(get("/api/v1/plots/{plotId}/telemetries", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].temperature", is(21.5)))
                .andExpect(jsonPath("$[2].humidity", is(63.0)))
                .andExpect(jsonPath("$[2].soilMoisture", is(20.7)))
                .andExpect(jsonPath("$[2].solarRadiation", is(476.0)));

        // 11. GET: Query telemetry series for a non-existent plot should fail with 404 ProblemDetail
        mockMvc.perform(get("/api/v1/plots/{plotId}/telemetries", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.title", is("Not Found")));
    }

    @Test
    @DisplayName("Should answer 404, not 500, for a path that no endpoint serves")
    void shouldReturnNotFoundForAnUnknownRoute() throws Exception {
        mockMvc.perform(get("/api/v1/plots/{plotId}/thinning-events", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("ROUTE_NOT_FOUND")))
                .andExpect(jsonPath("$.detail", containsString("/api/v1/plots/" + plotId + "/thinning-events")));
    }
}

package com.arcadiadevs.viora.platform.telemetry.interfaces.rest;

import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.ThresholdBreachInfo;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Two producers against the whole application: the alerts and the data hanging from a plot reach only
 * the producer who owns the plot. A new account must never see the demo producer's alerts.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:plotdataownership;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Plot data ownership integration")
class PlotDataOwnershipIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired AgroclimaticIncidentRepository incidentRepository;

    /** Open-Meteo stays out of the tests. */
    @MockitoBean HourlyWeatherProvider hourlyWeatherProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mvc;
    private final String ana = UUID.randomUUID().toString();
    private final String bruno = UUID.randomUUID().toString();
    private String anaPlot;
    private String anaIncident;
    private String anaStep;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();

        authenticateAs(ana);
        anaPlot = createPlot();
        var incident = incidentRepository.save(AgroclimaticIncident.raise(
                new PlotId(anaPlot),
                IncidentType.HEAT_WAVE,
                IncidentSeverity.CRITICAL,
                new ThresholdBreachInfo("AMBIENT_TEMPERATURE", 34.0, 32.0, "°C"),
                List.of("heat_wave.step.irrigate_early_morning"),
                Instant.now()
        ));
        anaIncident = incident.snapshot().id().incidentId();
        anaStep = incident.snapshot().mitigationSteps().getFirst().id().stepId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("The owner lists and opens the alerts of their plot")
    void ownerReachesTheirAlerts() throws Exception {
        authenticateAs(ana);

        mvc.perform(get("/api/v1/agroclimatic-incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents[*].id", hasItem(anaIncident)));
        mvc.perform(get("/api/v1/plots/{plotId}/agroclimatic-incidents", anaPlot))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents[*].id", hasItem(anaIncident)));
        mvc.perform(get("/api/v1/agroclimatic-incidents/{incidentId}", anaIncident))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Another producer's list leaves out alerts of plots that are not theirs")
    void anotherProducerDoesNotListTheAlerts() throws Exception {
        authenticateAs(bruno);

        mvc.perform(get("/api/v1/agroclimatic-incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents[*].id", not(hasItem(anaIncident))));
        mvc.perform(get("/api/v1/agroclimatic-incidents").param("plotId", anaPlot))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/plots/{plotId}/agroclimatic-incidents", anaPlot))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Another producer can neither open, postpone nor complete a step of the alert, which is left untouched")
    void anotherProducerCannotActOnTheAlert() throws Exception {
        authenticateAs(bruno);

        mvc.perform(get("/api/v1/agroclimatic-incidents/{incidentId}", anaIncident))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/agroclimatic-incidents/{incidentId}/postponements", anaIncident)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationHours\": 4}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/agroclimatic-incidents/{incidentId}/mitigation-steps/{stepId}", anaIncident, anaStep))
                .andExpect(status().isForbidden());

        var stored = incidentRepository.findAll(new PlotId(anaPlot), null, null).getFirst().snapshot();
        assertThat(stored.snoozedUntil()).isNull();
        assertThat(stored.mitigationSteps().getFirst().completed()).isFalse();
    }

    @Test
    @DisplayName("Another producer cannot read the sensors, readings, forecast, harvests or prescriptions of the plot")
    void anotherProducerCannotReadThePlotData() throws Exception {
        authenticateAs(bruno);

        for (var path : List.of("iot-devices", "telemetries", "forecasts", "harvest-records", "metrics", "thinning-prescriptions")) {
            mvc.perform(get("/api/v1/plots/{plotId}/" + path, anaPlot))
                    .andExpect(status().isForbidden());
        }
    }

    private String createPlot() throws Exception {
        var payload = """
                {
                  "name": "Cuartel Propio %s",
                  "variety": "ARBEQUINA",
                  "polygonGeoJson": "{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                  "rowSpacingM": 6.0,
                  "treeSpacingM": 4.0
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        var response = mvc.perform(post("/api/v1/plots").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private static void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}

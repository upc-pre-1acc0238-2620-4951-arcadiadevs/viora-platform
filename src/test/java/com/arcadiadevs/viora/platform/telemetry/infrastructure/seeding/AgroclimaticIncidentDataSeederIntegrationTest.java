package com.arcadiadevs.viora.platform.telemetry.infrastructure.seeding;

import com.arcadiadevs.viora.platform.orchard.infrastructure.seeding.OrchardDemoDataSeeder;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DisplayName("AgroclimaticIncidentDataSeeder & OrchardDemoDataSeeder Integration Tests")
class AgroclimaticIncidentDataSeederIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private OrchardDemoDataSeeder orchardDemoDataSeeder;

    @Autowired
    private AgroclimaticIncidentDataSeeder agroclimaticIncidentDataSeeder;

    @Autowired
    private AgroclimaticIncidentRepository incidentRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Should successfully seed demo plots and 4 incidents matching Figma T14/T15 specifications")
    void shouldSeedPlotsAndIncidentsSuccessfully() throws Exception {
        // 1. Verify list endpoint returns 4 incidents with correct summary counters
        mockMvc.perform(get("/api/v1/agroclimatic-incidents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.activeCount", is(2)))
                .andExpect(jsonPath("$.summary.criticalCount", is(1)))
                .andExpect(jsonPath("$.summary.warningCount", is(1)))
                .andExpect(jsonPath("$.summary.normalizedCount", is(2)))
                .andExpect(jsonPath("$.incidents", hasSize(4)));

        // 2. Find the active HEAT_WAVE incident
        var allIncidents = incidentRepository.findAll(null, null, null);
        var heatWaveIncidentOpt = allIncidents.stream()
                .filter(i -> i.snapshot().type() == IncidentType.HEAT_WAVE)
                .findFirst();

        assertThat(heatWaveIncidentOpt).isPresent();
        var heatWave = heatWaveIncidentOpt.get().snapshot();

        // 3. Verify detail endpoint for HEAT_WAVE matches Figma T15 (3 steps, step 1 completed, 7 trend points)
        mockMvc.perform(get("/api/v1/agroclimatic-incidents/" + heatWave.id().incidentId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(heatWave.id().incidentId().toString())))
                .andExpect(jsonPath("$.type", is("HEAT_WAVE")))
                .andExpect(jsonPath("$.severity", is("CRITICAL")))
                .andExpect(jsonPath("$.metricName", is("temperatura_maxima")))
                .andExpect(jsonPath("$.currentValue", is(34.0)))
                .andExpect(jsonPath("$.thresholdValue", is(32.0)))
                .andExpect(jsonPath("$.unit", is("°C")))
                .andExpect(jsonPath("$.mitigationSteps", hasSize(3)))
                .andExpect(jsonPath("$.mitigationSteps[0].completed", is(true)))
                .andExpect(jsonPath("$.mitigationSteps[0].completedAt", notNullValue()))
                .andExpect(jsonPath("$.mitigationSteps[1].completed", is(false)))
                .andExpect(jsonPath("$.mitigationSteps[2].completed", is(false)))
                .andExpect(jsonPath("$.weeklyTrend", hasSize(7)));
    }

    @Test
    @DisplayName("Should be completely idempotent when re-executing seeders")
    void shouldBeIdempotentOnRepeatedExecution() throws Exception {
        var initialIncidentsCount = incidentRepository.findAll(null, null, null).size();
        assertThat(initialIncidentsCount).isGreaterThanOrEqualTo(4);

        // Re-execute both seeders
        orchardDemoDataSeeder.run(null);
        agroclimaticIncidentDataSeeder.run(null);

        // Verify count has not increased
        var postExecutionCount = incidentRepository.findAll(null, null, null).size();
        assertThat(postExecutionCount).isEqualTo(initialIncidentsCount);
    }
}

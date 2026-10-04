package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import com.arcadiadevs.viora.platform.telemetry.application.commandservices.AgroclimaticIncidentCommandService;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.*;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncidentSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.CompleteMitigationStepCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.PostponeAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.entities.MitigationStepSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetAgroclimaticIncidentByIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetAgroclimaticIncidentsQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgroclimaticIncidentController REST Endpoint Integration Tests")
class AgroclimaticIncidentControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private AgroclimaticIncidentQueryService queryService;

    @Mock
    private AgroclimaticIncidentCommandService commandService;

    private final UUID incidentUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID plotUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID stepUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        var messageSource = new org.springframework.context.support.ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding("UTF-8");

        var controller = new AgroclimaticIncidentController(queryService, commandService, messageSource);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/agroclimatic-incidents should return 200 OK with summary and list")
    void shouldReturnSummaryAndIncidentListSuccessfully() throws Exception {
        var breach = new ThresholdBreachInfo("Soil Moisture", 18.5, 25.0, "%");
        var snapshot = new AgroclimaticIncidentSnapshot(
                new IncidentId(incidentUuid.toString()),
                new PlotId(plotUuid.toString()),
                IncidentType.HYDRIC_STRESS,
                IncidentSeverity.CRITICAL,
                IncidentStatus.ACTIVE,
                breach,
                Instant.parse("2026-10-01T10:00:00Z"),
                null,
                null,
                120L,
                List.of(new MitigationStepSnapshot(new MitigationStepId(stepUuid.toString()), "hydric_stress.step.check_drippers", false, null)),
                0L
        );
        var item = new AgroclimaticIncidentItem(snapshot, "Cuartel Norte", "Hass");
        var summary = new AgroclimaticIncidentsSummary(1L, 1L, 0L, 0L, List.of(item));

        when(queryService.handle(any(GetAgroclimaticIncidentsQuery.class)))
                .thenReturn(Result.success(summary));

        mockMvc.perform(get("/api/v1/agroclimatic-incidents")
                        .param("status", "ACTIVE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.activeCount", is(1)))
                .andExpect(jsonPath("$.summary.criticalCount", is(1)))
                .andExpect(jsonPath("$.summary.warningCount", is(0)))
                .andExpect(jsonPath("$.summary.normalizedCount", is(0)))
                .andExpect(jsonPath("$.incidents", hasSize(1)))
                .andExpect(jsonPath("$.incidents[0].id", is(incidentUuid.toString())))
                .andExpect(jsonPath("$.incidents[0].plotName", is("Cuartel Norte")))
                .andExpect(jsonPath("$.incidents[0].type", is("HYDRIC_STRESS")))
                .andExpect(jsonPath("$.incidents[0].severity", is("CRITICAL")))
                .andExpect(jsonPath("$.incidents[0].metricName", is("Soil Moisture")));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/agroclimatic-incidents should return 200 OK with plot incidents")
    void shouldReturnPlotIncidentsSuccessfully() throws Exception {
        var breach = new ThresholdBreachInfo("Ambient Temperature", 38.2, 35.0, "°C");
        var snapshot = new AgroclimaticIncidentSnapshot(
                new IncidentId(incidentUuid.toString()),
                new PlotId(plotUuid.toString()),
                IncidentType.HEAT_WAVE,
                IncidentSeverity.WARNING,
                IncidentStatus.ACTIVE,
                breach,
                Instant.parse("2026-10-02T14:00:00Z"),
                null,
                null,
                60L,
                List.of(),
                0L
        );
        var item = new AgroclimaticIncidentItem(snapshot, "Cuartel Sur", "Arbequina");
        var summary = new AgroclimaticIncidentsSummary(1L, 0L, 1L, 0L, List.of(item));

        when(queryService.handle(any(GetAgroclimaticIncidentsQuery.class)))
                .thenReturn(Result.success(summary));

        mockMvc.perform(get("/api/v1/plots/{plotId}/agroclimatic-incidents", plotUuid)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(1)))
                .andExpect(jsonPath("$.incidents[0].type", is("HEAT_WAVE")));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/agroclimatic-incidents should return 404 when plot not found")
    void shouldReturn404WhenPlotNotFound() throws Exception {
        when(queryService.handle(any(GetAgroclimaticIncidentsQuery.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("plot.not_found", "PLOT")));

        mockMvc.perform(get("/api/v1/plots/{plotId}/agroclimatic-incidents", plotUuid)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/agroclimatic-incidents/{incidentId} should return 200 OK with full detail")
    void shouldReturnIncidentDetailSuccessfully() throws Exception {
        var breach = new ThresholdBreachInfo("Soil Moisture", 18.5, 25.0, "%");
        var step = new MitigationStepSnapshot(
                new MitigationStepId(stepUuid.toString()),
                "hydric_stress.step.check_drippers",
                false,
                null
        );
        var snapshot = new AgroclimaticIncidentSnapshot(
                new IncidentId(incidentUuid.toString()),
                new PlotId(plotUuid.toString()),
                IncidentType.HYDRIC_STRESS,
                IncidentSeverity.CRITICAL,
                IncidentStatus.ACTIVE,
                breach,
                Instant.parse("2026-10-01T10:00:00Z"),
                null,
                null,
                120L,
                List.of(step),
                0L
        );
        var trendPoint = new WeeklyTrendPoint(Instant.parse("2026-10-01T00:00:00Z"), 18.5, 25.0);

        var detail = new AgroclimaticIncidentDetail(
                snapshot,
                "Cuartel Norte",
                "Hass",
                List.of(trendPoint)
        );

        when(queryService.handle(any(GetAgroclimaticIncidentByIdQuery.class)))
                .thenReturn(Result.success(detail));

        mockMvc.perform(get("/api/v1/agroclimatic-incidents/{incidentId}", incidentUuid)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(incidentUuid.toString())))
                .andExpect(jsonPath("$.plotVariety", is("Hass")))
                .andExpect(jsonPath("$.mitigationSteps", hasSize(1)))
                .andExpect(jsonPath("$.mitigationSteps[0].instructionKey", is("hydric_stress.step.check_drippers")))
                .andExpect(jsonPath("$.weeklyTrend", hasSize(1)))
                .andExpect(jsonPath("$.weeklyTrend[0].value", is(18.5)));
    }

    @Test
    @DisplayName("GET /api/v1/agroclimatic-incidents/{incidentId} should resolve adaptive instructions according to locale")
    void shouldResolveAdaptiveInstructionsAccordingToLocale() throws Exception {
        var breach = new ThresholdBreachInfo("Ambient Temperature", 38.0, 36.0, "°C");
        // 2026-11-20 is a Thursday in America/Lima
        var step1 = new MitigationStepSnapshot(
                new MitigationStepId(stepUuid.toString()),
                "heat_wave.step.pre_irrigation",
                true,
                Instant.parse("2026-11-19T18:00:00Z")
        );
        var step2 = new MitigationStepSnapshot(
                new MitigationStepId(UUID.randomUUID().toString()),
                "heat_wave.step.morning_irrigation",
                false,
                null
        );
        var step3 = new MitigationStepSnapshot(
                new MitigationStepId(UUID.randomUUID().toString()),
                "heat_wave.step.suspend_cultural_ops",
                false,
                null
        );

        var snapshot = new AgroclimaticIncidentSnapshot(
                new IncidentId(incidentUuid.toString()),
                new PlotId(plotUuid.toString()),
                IncidentType.HEAT_WAVE,
                IncidentSeverity.CRITICAL,
                IncidentStatus.ACTIVE,
                breach,
                Instant.parse("2026-11-19T14:00:00Z"),
                null,
                null,
                180L,
                List.of(step1, step2, step3),
                0L
        );

        var detail = new AgroclimaticIncidentDetail(
                snapshot,
                "La Yarada 02",
                "Criolla",
                List.of()
        );

        when(queryService.handle(any(GetAgroclimaticIncidentByIdQuery.class)))
                .thenReturn(Result.success(detail));

        // Test in Spanish
        mockMvc.perform(get("/api/v1/agroclimatic-incidents/{incidentId}", incidentUuid)
                        .header("Accept-Language", "es")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mitigationSteps[0].instruction", is("Riega el miércoles por la tarde")))
                .andExpect(jsonPath("$.mitigationSteps[0].completed", is(true)))
                .andExpect(jsonPath("$.mitigationSteps[1].instruction", is("Riega el jueves antes de las 9 a. m.")))
                .andExpect(jsonPath("$.mitigationSteps[1].completed", is(false)))
                .andExpect(jsonPath("$.mitigationSteps[2].instruction", is("No podes ni aclares ese día")));

        // Test in English
        mockMvc.perform(get("/api/v1/agroclimatic-incidents/{incidentId}", incidentUuid)
                        .header("Accept-Language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mitigationSteps[0].instruction", is("Irrigate on Wednesday afternoon")))
                .andExpect(jsonPath("$.mitigationSteps[1].instruction", is("Irrigate on Thursday before 9:00 AM")))
                .andExpect(jsonPath("$.mitigationSteps[2].instruction", is("Do not prune or thin on that day")));
    }

    @Test
    @DisplayName("GET /api/v1/agroclimatic-incidents/{incidentId} should return 404 when not found")
    void shouldReturn404WhenIncidentNotFound() throws Exception {
        when(queryService.handle(any(GetAgroclimaticIncidentByIdQuery.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("incident.not_found", "INCIDENT")));

        mockMvc.perform(get("/api/v1/agroclimatic-incidents/{incidentId}", incidentUuid)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/agroclimatic-incidents/{incidentId}/postponements should return 200 OK on success")
    void shouldPostponeIncidentSuccessfully() throws Exception {
        when(commandService.handle(any(PostponeAgroclimaticIncidentCommand.class)))
                .thenReturn(Result.success(incidentUuid.toString()));

        var payload = """
                {
                    "durationHours": 6
                }
                """;

        mockMvc.perform(post("/api/v1/agroclimatic-incidents/{incidentId}/postponements", incidentUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("incident.postponed_successfully")));
    }

    @Test
    @DisplayName("PUT /api/v1/agroclimatic-incidents/{incidentId}/mitigation-steps/{stepId} should return 200 OK on success")
    void shouldCompleteMitigationStepSuccessfully() throws Exception {
        when(commandService.handle(any(CompleteMitigationStepCommand.class)))
                .thenReturn(Result.success(stepUuid.toString()));

        mockMvc.perform(put("/api/v1/agroclimatic-incidents/{incidentId}/mitigation-steps/{stepId}", incidentUuid, stepUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("mitigation_step.completed_successfully")));
    }
}

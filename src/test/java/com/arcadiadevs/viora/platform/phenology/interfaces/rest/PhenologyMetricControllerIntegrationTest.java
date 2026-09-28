package com.arcadiadevs.viora.platform.phenology.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import com.arcadiadevs.viora.platform.phenology.application.queryservices.PhenologyMetricQueryService;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetPlotMetricsQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricEvaluationResult;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricType;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.controllers.PhenologyMetricController;
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
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PhenologyMetricController REST Endpoint Integration Tests")
class PhenologyMetricControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private PhenologyMetricQueryService metricQueryService;

    private final UUID plotId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @BeforeEach
    void setUp() {
        var controller = new PhenologyMetricController(metricQueryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/metrics - Should return 200 OK with all evaluated metrics")
    void shouldReturnAllMetricsWhenNoFilterSpecified() throws Exception {
        var bbiResult = new MetricEvaluationResult(
                MetricType.BIENNIAL_BEARING_INDEX,
                0.42,
                "MODERATE_ALTERNATION",
                Map.of("formula", "Hoblyn (1936)", "evaluatedYearsCount", 4, "sampleSufficiency", "SUFFICIENT"),
                Instant.parse("2026-09-28T14:00:00Z")
        );
        var chillingResult = new MetricEvaluationResult(
                MetricType.EREZ_CHILLING_PORTIONS,
                28.5,
                "SATISFIED",
                Map.of("model", "Dynamic Erez-Fishman", "thresholdTarget", 27.0, "completionPercentage", 105.56),
                Instant.parse("2026-09-28T14:00:00Z")
        );

        when(metricQueryService.handle(any(GetPlotMetricsQuery.class)))
                .thenReturn(Result.success(List.of(bbiResult, chillingResult)));

        mockMvc.perform(get("/api/v1/plots/{plotId}/metrics", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].metricName", is("BIENNIAL_BEARING_INDEX")))
                .andExpect(jsonPath("$[0].value", is(0.42)))
                .andExpect(jsonPath("$[0].qualitativeCategory", is("MODERATE_ALTERNATION")))
                .andExpect(jsonPath("$[0].details.formula", is("Hoblyn (1936)")))
                .andExpect(jsonPath("$[1].metricName", is("EREZ_CHILLING_PORTIONS")))
                .andExpect(jsonPath("$[1].value", is(28.5)))
                .andExpect(jsonPath("$[1].qualitativeCategory", is("SATISFIED")));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/metrics?metricName=BBI - Should return 200 OK with filtered BBI metric")
    void shouldReturnFilteredBbiMetric() throws Exception {
        var bbiResult = new MetricEvaluationResult(
                MetricType.BIENNIAL_BEARING_INDEX,
                0.42,
                "MODERATE_ALTERNATION",
                Map.of("formula", "Hoblyn (1936)"),
                Instant.parse("2026-09-28T14:00:00Z")
        );

        when(metricQueryService.handle(any(GetPlotMetricsQuery.class)))
                .thenReturn(Result.success(List.of(bbiResult)));

        mockMvc.perform(get("/api/v1/plots/{plotId}/metrics", plotId)
                        .param("metricName", "BBI")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].metricName", is("BIENNIAL_BEARING_INDEX")));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/metrics?name=CHILLING - Should support 'name' alias and return 200 OK")
    void shouldSupportNameAliasQueryParam() throws Exception {
        var chillingResult = new MetricEvaluationResult(
                MetricType.EREZ_CHILLING_PORTIONS,
                28.5,
                "SATISFIED",
                Map.of("model", "Dynamic Erez-Fishman"),
                Instant.parse("2026-09-28T14:00:00Z")
        );

        when(metricQueryService.handle(any(GetPlotMetricsQuery.class)))
                .thenReturn(Result.success(List.of(chillingResult)));

        mockMvc.perform(get("/api/v1/plots/{plotId}/metrics", plotId)
                        .param("name", "CHILLING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].metricName", is("EREZ_CHILLING_PORTIONS")));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/metrics - Should return 400 Bad Request on invalid metric name")
    void shouldReturnBadRequestOnInvalidMetricName() throws Exception {
        mockMvc.perform(get("/api/v1/plots/{plotId}/metrics", plotId)
                        .param("metricName", "NON_EXISTENT_METRIC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/metrics - Should return 400 Bad Request on malformed plot UUID")
    void shouldReturnBadRequestOnMalformedPlotId() throws Exception {
        mockMvc.perform(get("/api/v1/plots/{plotId}/metrics", "not-a-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/metrics - Should return 404 Not Found when plot is not found in ACL")
    void shouldReturnNotFoundWhenPlotDoesNotExistInAcl() throws Exception {
        when(metricQueryService.handle(any(GetPlotMetricsQuery.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("Plot", plotId.toString())));

        mockMvc.perform(get("/api/v1/plots/{plotId}/metrics", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetPlotSamplingStatesQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetPlotSamplingStatesQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotSamplingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
@DisplayName("PlotSamplingCollectionController REST Endpoint Integration Tests")
class PlotSamplingCollectionControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private GetPlotSamplingStatesQueryService queryService;

    private final String defaultActorId = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        var controller = new PlotSamplingCollectionController(queryService, defaultActorId);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/samplings - 200 OK returning plots with sampling progress")
    void shouldReturnPlotSamplingStatesSuccessfully() throws Exception {
        var state1 = new PlotSamplingState(
                new PlotId(UUID.fromString("550e8400-e29b-41d4-a716-446655440001").toString()),
                "Lote A",
                "Sevillana",
                4.5,
                new CampaignYear(2026),
                "COMPLETED",
                5,
                0,
                true
        );

        var state2 = new PlotSamplingState(
                new PlotId(UUID.fromString("550e8400-e29b-41d4-a716-446655440002").toString()),
                "Lote B",
                "Arbequina",
                3.0,
                new CampaignYear(2026),
                "IN_PROGRESS",
                2,
                3,
                false
        );

        var state3 = new PlotSamplingState(
                new PlotId(UUID.fromString("550e8400-e29b-41d4-a716-446655440003").toString()),
                "Lote C",
                "Criolla",
                2.2,
                new CampaignYear(2026),
                "NOT_STARTED",
                0,
                5,
                false
        );

        when(queryService.handle(any(GetPlotSamplingStatesQuery.class)))
                .thenReturn(Result.success(List.of(state1, state2, state3)));

        mockMvc.perform(get("/api/v1/samplings")
                        .param("campaignYear", "2026")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].plotName", is("Lote A")))
                .andExpect(jsonPath("$[0].variety", is("Sevillana")))
                .andExpect(jsonPath("$[0].samplingStatus", is("COMPLETED")))
                .andExpect(jsonPath("$[0].areaHectares", is(4.5)))
                .andExpect(jsonPath("$[0].sampledTreesCount", is(5)))
                .andExpect(jsonPath("$[0].isRepresentative", is(true)))
                .andExpect(jsonPath("$[1].plotName", is("Lote B")))
                .andExpect(jsonPath("$[1].samplingStatus", is("IN_PROGRESS")))
                .andExpect(jsonPath("$[1].sampledTreesCount", is(2)))
                .andExpect(jsonPath("$[1].treesNeeded", is(3)))
                .andExpect(jsonPath("$[2].plotName", is("Lote C")))
                .andExpect(jsonPath("$[2].samplingStatus", is("NOT_STARTED")))
                .andExpect(jsonPath("$[2].sampledTreesCount", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/samplings - 400 Bad Request when query service returns validation error")
    void shouldReturnBadRequestWhenQueryServiceReturnsValidationFailure() throws Exception {
        when(queryService.handle(any(GetPlotSamplingStatesQuery.class)))
                .thenReturn(Result.failure(ApplicationError.validationError("campaignYear", "Campaign year must be valid")));

        mockMvc.perform(get("/api/v1/samplings")
                        .param("campaignYear", "2026")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("GET /api/v1/samplings - 400 Bad Request on malformed campaign year")
    void shouldReturnBadRequestOnMalformedCampaignYear() throws Exception {
        mockMvc.perform(get("/api/v1/samplings")
                        .param("campaignYear", "invalid-year")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}

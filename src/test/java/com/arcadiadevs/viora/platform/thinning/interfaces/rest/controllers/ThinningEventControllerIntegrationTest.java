package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetThinningEventsQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetThinningEventsQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.ThinningEvent;
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
@DisplayName("ThinningEventController REST Endpoint Integration Tests")
class ThinningEventControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private GetThinningEventsQueryService queryService;

    private final UUID plotId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private final String defaultActorId = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        var controller = new ThinningEventController(queryService, defaultActorId);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/thinning-events - 200 OK returning milestone events")
    void shouldReturnThinningEventsSuccessfully() throws Exception {
        var event1 = new ThinningEvent(
                "a1b2c3d4-0001-4000-8000-000000000001",
                "SAMPLING_COMPLETED",
                "f1e2d3c4-1111-4000-8000-000000000001",
                null,
                new PlotId(plotId.toString()),
                "La Yarada 02",
                new CampaignYear(2026),
                Instant.parse("2026-11-18T09:30:00Z"),
                5,
                60,
                37,
                0.62,
                true,
                null,
                null,
                null,
                null,
                null
        );

        var event2 = new ThinningEvent(
                "a1b2c3d4-0002-4000-8000-000000000002",
                "THINNING_EXECUTED",
                "f1e2d3c4-1111-4000-8000-000000000001",
                "c1b2a3d4-2222-4000-8000-000000000002",
                new PlotId(plotId.toString()),
                "La Yarada 02",
                new CampaignYear(2026),
                Instant.parse("2026-11-14T15:00:00Z"),
                null,
                null,
                null,
                null,
                null,
                30.0,
                420.0,
                LocalDate.parse("2026-11-14"),
                4,
                "OPTIMAL"
        );

        when(queryService.handle(any(GetThinningEventsQuery.class)))
                .thenReturn(Result.success(List.of(event1, event2)));

        mockMvc.perform(get("/api/v1/thinning-events")
                        .param("campaignYear", "2026")
                        .param("plotId", plotId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(2)))
                .andExpect(jsonPath("$.events[0].id", is("a1b2c3d4-0001-4000-8000-000000000001")))
                .andExpect(jsonPath("$.events[0].eventType", is("SAMPLING_COMPLETED")))
                .andExpect(jsonPath("$.events[0].plotName", is("La Yarada 02")))
                .andExpect(jsonPath("$.events[0].evaluatedTreesCount", is(5)))
                .andExpect(jsonPath("$.events[0].totalFruitsCount", is(37)))
                .andExpect(jsonPath("$.events[1].eventType", is("THINNING_EXECUTED")))
                .andExpect(jsonPath("$.events[1].removalPercentage", is(30.0)))
                .andExpect(jsonPath("$.events[1].timeliness", is("OPTIMAL")));
    }

    @Test
    @DisplayName("GET /api/v1/thinning-events - 404 Not Found when plot does not exist")
    void shouldReturnNotFoundWhenPlotDoesNotExist() throws Exception {
        when(queryService.handle(any(GetThinningEventsQuery.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("Plot", plotId.toString())));

        mockMvc.perform(get("/api/v1/thinning-events")
                        .param("plotId", plotId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PLOT_NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /api/v1/thinning-events - 400 Bad Request on malformed plotId UUID")
    void shouldReturnBadRequestOnMalformedPlotId() throws Exception {
        mockMvc.perform(get("/api/v1/thinning-events")
                        .param("plotId", "invalid-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}

package com.arcadiadevs.viora.platform.phenology.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import com.arcadiadevs.viora.platform.phenology.application.commandservices.HarvestRecordCommandService;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RecordHarvestYieldCommand;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestYield;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.controllers.HarvestRecordController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("HarvestRecordController REST Endpoint Integration Tests")
class HarvestRecordControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private HarvestRecordCommandService harvestRecordCommandService;

    @Mock
    private ChillAccumulationTrackerRepository trackerRepository;

    private final UUID plotId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @BeforeEach
    void setUp() {
        var controller = new HarvestRecordController(harvestRecordCommandService, trackerRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/plots/{plotId}/harvest-records should return 201 Created on valid input")
    void shouldReturnCreatedWhenPayloadIsValid() throws Exception {
        var tracker = ChillAccumulationTracker.create(new PlotId(plotId.toString()), new CampaignYear(2025));
        var entry = tracker.recordHarvest(new CampaignYear(2025), new HarvestYield(14250.0, 8200.0, 6050.0));
        var entryId = entry.snapshot().id().harvestEntryId();

        when(harvestRecordCommandService.handle(any(RecordHarvestYieldCommand.class)))
                .thenReturn(Result.success(entryId));
        when(trackerRepository.findByPlotId(any(PlotId.class)))
                .thenReturn(Optional.of(tracker));

        String payload = """
                {
                    "campaignYear": 2025,
                    "totalYieldKg": 14250.0,
                    "greenKg": 8200.0,
                    "blackKg": 6050.0,
                    "notes": "Optimal sanitary condition"
                }
                """;

        mockMvc.perform(post("/api/v1/plots/{plotId}/harvest-records", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(entryId)))
                .andExpect(jsonPath("$.plotId", is(plotId.toString())))
                .andExpect(jsonPath("$.campaignYear", is(2025)))
                .andExpect(jsonPath("$.totalYieldKg", is(14250.0)))
                .andExpect(jsonPath("$.greenKg", is(8200.0)))
                .andExpect(jsonPath("$.blackKg", is(6050.0)));
    }

    @Test
    @DisplayName("POST /api/v1/plots/{plotId}/harvest-records should return 400 Bad Request on incoherent yield sum")
    void shouldReturnBadRequestWhenIncoherentSum() throws Exception {
        String payload = """
                {
                    "campaignYear": 2025,
                    "totalYieldKg": 5000.0,
                    "greenKg": 4000.0,
                    "blackKg": 2000.0
                }
                """;

        when(harvestRecordCommandService.handle(any(RecordHarvestYieldCommand.class)))
                .thenReturn(Result.failure(ApplicationError.validationError("yield", "phenology.harvest_yield.incoherent_sum")));

        mockMvc.perform(post("/api/v1/plots/{plotId}/harvest-records", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/plots/{plotId}/harvest-records should return 409 Conflict when duplicate campaign year")
    void shouldReturnConflictWhenDuplicateCampaignYear() throws Exception {
        when(harvestRecordCommandService.handle(any(RecordHarvestYieldCommand.class)))
                .thenReturn(Result.failure(ApplicationError.conflict("harvest", "phenology.harvest_yield.duplicate_campaign")));

        String payload = """
                {
                    "campaignYear": 2025,
                    "totalYieldKg": 10000.0
                }
                """;

        mockMvc.perform(post("/api/v1/plots/{plotId}/harvest-records", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }
}

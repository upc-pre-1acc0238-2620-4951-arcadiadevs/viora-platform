package com.arcadiadevs.viora.platform.phenology.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import com.arcadiadevs.viora.platform.phenology.application.commandservices.HarvestRecordCommandService;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RecordHarvestYieldCommand;
import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RemoveHarvestRecordCommand;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestYield;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.controllers.HarvestRecordController;
import com.arcadiadevs.viora.platform.phenology.application.queryservices.HarvestRecordQueryService;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetHarvestRecordsByPlotIdQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private HarvestRecordQueryService harvestRecordQueryService;

    @Mock
    private ChillAccumulationTrackerRepository trackerRepository;

    private final UUID plotId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @BeforeEach
    void setUp() {
        var controller = new HarvestRecordController(harvestRecordCommandService, harvestRecordQueryService, trackerRepository);
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

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/harvest-records should return 200 OK with harvest record list")
    void shouldReturnOkWithHarvestRecordsList() throws Exception {
        var tracker = ChillAccumulationTracker.create(new PlotId(plotId.toString()), new CampaignYear(2024));
        var entry1 = tracker.recordHarvest(new CampaignYear(2024), new HarvestYield(12000.0, 7000.0, 5000.0));
        var entry2 = tracker.recordHarvest(new CampaignYear(2025), new HarvestYield(15000.0, 9000.0, 6000.0));

        when(harvestRecordQueryService.handle(any(GetHarvestRecordsByPlotIdQuery.class)))
                .thenReturn(Result.success(List.of(entry1.snapshot(), entry2.snapshot())));
        when(trackerRepository.findByPlotId(any(PlotId.class)))
                .thenReturn(Optional.of(tracker));

        mockMvc.perform(get("/api/v1/plots/{plotId}/harvest-records", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].campaignYear", is(2024)))
                .andExpect(jsonPath("$[0].totalYieldKg", is(12000.0)))
                .andExpect(jsonPath("$[1].campaignYear", is(2025)))
                .andExpect(jsonPath("$[1].totalYieldKg", is(15000.0)));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/harvest-records with campaignYear should return filtered 200 OK")
    void shouldReturnOkWithFilteredHarvestRecord() throws Exception {
        var tracker = ChillAccumulationTracker.create(new PlotId(plotId.toString()), new CampaignYear(2025));
        var entry = tracker.recordHarvest(new CampaignYear(2025), new HarvestYield(14250.0, 8200.0, 6050.0));

        when(harvestRecordQueryService.handle(any(GetHarvestRecordsByPlotIdQuery.class)))
                .thenReturn(Result.success(List.of(entry.snapshot())));
        when(trackerRepository.findByPlotId(any(PlotId.class)))
                .thenReturn(Optional.of(tracker));

        mockMvc.perform(get("/api/v1/plots/{plotId}/harvest-records?campaignYear=2025", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].campaignYear", is(2025)))
                .andExpect(jsonPath("$[0].totalYieldKg", is(14250.0)));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/harvest-records should return 200 OK with empty array when no records")
    void shouldReturnEmptyListWhenNoRecords() throws Exception {
        when(harvestRecordQueryService.handle(any(GetHarvestRecordsByPlotIdQuery.class)))
                .thenReturn(Result.success(Collections.emptyList()));
        when(trackerRepository.findByPlotId(any(PlotId.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/plots/{plotId}/harvest-records", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/harvest-records should return 404 Not Found when plot does not exist in ACL")
    void shouldReturnNotFoundWhenPlotDoesNotExistInAcl() throws Exception {
        when(harvestRecordQueryService.handle(any(GetHarvestRecordsByPlotIdQuery.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("Plot", plotId.toString())));

        mockMvc.perform(get("/api/v1/plots/{plotId}/harvest-records", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/plots/{plotId}/harvest-records should return 400 Bad Request on invalid campaign year")
    void shouldReturnBadRequestOnInvalidCampaignYear() throws Exception {
        mockMvc.perform(get("/api/v1/plots/{plotId}/harvest-records?campaignYear=1900", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId}/harvest-records/{recordId} should return 200 OK and ETag when valid")
    void shouldReturnOkWhenRectifyingHarvestRecordIsValid() throws Exception {
        var tracker = ChillAccumulationTracker.create(new PlotId(plotId.toString()), new CampaignYear(2025));
        var entry = tracker.recordHarvest(new CampaignYear(2025), new HarvestYield(14250.0, 8200.0, 6050.0));
        var recordId = entry.snapshot().id().harvestEntryId();

        when(harvestRecordCommandService.handle(any(com.arcadiadevs.viora.platform.phenology.domain.model.commands.RectifyHarvestYieldCommand.class)))
                .thenReturn(Result.success(recordId));
        when(trackerRepository.findByPlotId(any(PlotId.class)))
                .thenReturn(Optional.of(tracker));

        String payload = """
                {
                    "totalYieldKg": 14500.0,
                    "greenKg": 8300.0,
                    "blackKg": 6200.0,
                    "notes": "Correction after recalibration"
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordId)
                        .header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(recordId)))
                .andExpect(jsonPath("$.totalYieldKg", is(14250.0)));
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId}/harvest-records/{recordId} should return 412 Precondition Failed when revision mismatches")
    void shouldReturnPreconditionFailedWhenRevisionMismatches() throws Exception {
        var recordId = UUID.randomUUID().toString();

        when(harvestRecordCommandService.handle(any(com.arcadiadevs.viora.platform.phenology.domain.model.commands.RectifyHarvestYieldCommand.class)))
                .thenReturn(Result.failure(ApplicationError.preconditionFailed("tracker", "phenology.tracker.revision.mismatch")));

        String payload = """
                {
                    "totalYieldKg": 14500.0,
                    "greenKg": 8300.0,
                    "blackKg": 6200.0
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordId)
                        .header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId}/harvest-records/{recordId} should return 400 Bad Request on malformed If-Match")
    void shouldReturnBadRequestOnMalformedIfMatch() throws Exception {
        var recordId = UUID.randomUUID().toString();

        String payload = """
                {
                    "totalYieldKg": 14500.0,
                    "greenKg": 8300.0,
                    "blackKg": 6200.0
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordId)
                        .header("If-Match", "abc-invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId}/harvest-records/{recordId} should return 404 Not Found when harvest record not found")
    void shouldReturnNotFoundWhenHarvestRecordNotFound() throws Exception {
        var recordId = UUID.randomUUID().toString();

        when(harvestRecordCommandService.handle(any(com.arcadiadevs.viora.platform.phenology.domain.model.commands.RectifyHarvestYieldCommand.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("HarvestRecord", recordId)));

        String payload = """
                {
                    "totalYieldKg": 14500.0,
                    "greenKg": 8300.0,
                    "blackKg": 6200.0
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/plots/{plotId}/harvest-records/{recordId} should return 200 OK with a confirmation message")
    void shouldReturnOkWhenRemovingHarvestRecord() throws Exception {
        var recordId = UUID.randomUUID().toString();

        when(harvestRecordCommandService.handle(any(RemoveHarvestRecordCommand.class)))
                .thenReturn(Result.success(recordId));

        mockMvc.perform(delete("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordId)
                        .header("If-Match", "\"2\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Harvest record deleted successfully")));

        var captor = ArgumentCaptor.forClass(RemoveHarvestRecordCommand.class);
        verify(harvestRecordCommandService).handle(captor.capture());
        assertThat(captor.getValue().plotId()).isEqualTo(plotId.toString());
        assertThat(captor.getValue().recordId()).isEqualTo(recordId);
        assertThat(captor.getValue().expectedRevision()).isEqualTo(2L);
    }

    @Test
    @DisplayName("DELETE /api/v1/plots/{plotId}/harvest-records/{recordId} should not require If-Match")
    void shouldReturnOkWhenRemovingWithoutIfMatch() throws Exception {
        var recordId = UUID.randomUUID().toString();

        when(harvestRecordCommandService.handle(any(RemoveHarvestRecordCommand.class)))
                .thenReturn(Result.success(recordId));

        mockMvc.perform(delete("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordId))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(RemoveHarvestRecordCommand.class);
        verify(harvestRecordCommandService).handle(captor.capture());
        assertThat(captor.getValue().expectedRevision()).isNull();
    }

    @Test
    @DisplayName("DELETE /api/v1/plots/{plotId}/harvest-records/{recordId} should return 404 Not Found when the record does not exist")
    void shouldReturnNotFoundWhenRemovingUnknownHarvestRecord() throws Exception {
        var recordId = UUID.randomUUID().toString();

        when(harvestRecordCommandService.handle(any(RemoveHarvestRecordCommand.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("HarvestRecord", recordId)));

        mockMvc.perform(delete("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/plots/{plotId}/harvest-records/{recordId} should return 412 Precondition Failed when revision mismatches")
    void shouldReturnPreconditionFailedWhenRemovingWithRevisionMismatch() throws Exception {
        var recordId = UUID.randomUUID().toString();

        when(harvestRecordCommandService.handle(any(RemoveHarvestRecordCommand.class)))
                .thenReturn(Result.failure(ApplicationError.preconditionFailed("tracker", "phenology.tracker.revision.mismatch")));

        mockMvc.perform(delete("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordId)
                        .header("If-Match", "\"1\""))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    @DisplayName("DELETE /api/v1/plots/{plotId}/harvest-records/{recordId} should return 400 Bad Request on malformed If-Match")
    void shouldReturnBadRequestWhenRemovingWithMalformedIfMatch() throws Exception {
        var recordId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordId)
                        .header("If-Match", "abc-invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(harvestRecordCommandService);
    }
}


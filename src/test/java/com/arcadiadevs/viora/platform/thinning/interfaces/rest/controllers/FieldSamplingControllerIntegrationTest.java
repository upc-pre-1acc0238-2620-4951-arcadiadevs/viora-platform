package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import com.arcadiadevs.viora.platform.thinning.application.commandservices.FruitThinningPrescriptionCommandService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.IngestFieldSamplingsBatchCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingBatchId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FieldSamplingController REST Endpoint Integration Tests")
class FieldSamplingControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private FruitThinningPrescriptionCommandService thinningCommandService;

    private final UUID plotId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @BeforeEach
    void setUp() {
        var controller = new FieldSamplingController(thinningCommandService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/plots/{plotId}/samplings - 201 Created on valid sampling batch")
    void shouldReturnCreatedOnValidSamplingBatch() throws Exception {
        var prescription = FruitThinningPrescription.createForPlot(
                new PlotId(plotId.toString()),
                new CampaignYear(2026),
                1L
        );
        var actorId = new UserId(UUID.randomUUID().toString());
        prescription.ingestSamplingsBatch(
                actorId,
                new SamplingBatchId("d3b07384-d113-496e-bc35-cf21eb943c5a"),
                List.of(
                        TreeSamplingRecord.create("T-01", 10, 100, 150.0, LocalDate.now()),
                        TreeSamplingRecord.create("T-02", 10, 100, 150.0, LocalDate.now()),
                        TreeSamplingRecord.create("T-03", 10, 100, 150.0, LocalDate.now()),
                        TreeSamplingRecord.create("T-04", 10, 100, 150.0, LocalDate.now()),
                        TreeSamplingRecord.create("T-05", 14, 140, 150.0, LocalDate.now())
                )
        );

        when(thinningCommandService.handle(any(IngestFieldSamplingsBatchCommand.class)))
                .thenReturn(Result.success(prescription));

        String requestBody = """
                {
                  "clientBatchId": "d3b07384-d113-496e-bc35-cf21eb943c5a",
                  "actorId": "550e8400-e29b-41d4-a716-446655440000",
                  "campaignYear": 2026,
                  "samples": [
                    {
                      "treeTag": "T-042",
                      "shootCount": 12,
                      "fruitSetCount": 144,
                      "trunkDiameterMm": 185.0,
                      "samplingDate": "2026-05-15"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/plots/{plotId}/samplings", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plotId", is(plotId.toString())))
                .andExpect(jsonPath("$.campaignYear", is(2026)))
                .andExpect(jsonPath("$.sampledTreesCount", is(5)))
                .andExpect(jsonPath("$.isRepresentative", is(true)))
                .andExpect(jsonPath("$.treesNeeded", is(0)));
    }

    @Test
    @DisplayName("POST /api/v1/plots/{plotId}/samplings - 404 Not Found when plot does not exist")
    void shouldReturnNotFoundWhenPlotDoesNotExist() throws Exception {
        when(thinningCommandService.handle(any(IngestFieldSamplingsBatchCommand.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("Plot", plotId.toString())));

        String requestBody = """
                {
                  "clientBatchId": "d3b07384-d113-496e-bc35-cf21eb943c5a",
                  "actorId": "550e8400-e29b-41d4-a716-446655440000",
                  "campaignYear": 2026,
                  "samples": [
                    {
                      "treeTag": "T-042",
                      "shootCount": 12,
                      "fruitSetCount": 144,
                      "trunkDiameterMm": 185.0,
                      "samplingDate": "2026-05-15"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/plots/{plotId}/samplings", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PLOT_NOT_FOUND")));
    }

    @Test
    @DisplayName("POST /api/v1/plots/{plotId}/samplings - 400 Bad Request on invalid arguments")
    void shouldReturnBadRequestOnMissingFields() throws Exception {
        String invalidBody = """
                {
                  "clientBatchId": "",
                  "actorId": "550e8400-e29b-41d4-a716-446655440000",
                  "campaignYear": 2026,
                  "samples": []
                }
                """;

        mockMvc.perform(post("/api/v1/plots/{plotId}/samplings", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }
}

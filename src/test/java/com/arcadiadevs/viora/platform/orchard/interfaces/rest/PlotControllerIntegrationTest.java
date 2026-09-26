package com.arcadiadevs.viora.platform.orchard.interfaces.rest;

import com.arcadiadevs.viora.platform.orchard.application.commandservices.PlotCommandService;
import com.arcadiadevs.viora.platform.orchard.application.queryservices.PlotQueryService;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.UpdatePlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.controllers.PlotController;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlotController REST Endpoint Integration Tests")
class PlotControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private PlotCommandService plotCommandService;

    @Mock
    private PlotQueryService plotQueryService;

    @Mock
    private PlotRepository plotRepository;

    private final UUID producerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private final String validGeoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}";

    @BeforeEach
    void setUp() {
        var plotController = new PlotController(plotCommandService, plotQueryService, plotRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(plotController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/plots should return 201 Created and Location header on valid input")
    void shouldReturnCreatedWhenPayloadIsValid() throws Exception {
        var plot = Plot.delimit(
                new ProducerId(producerId.toString()),
                new PlotName("Cuartel San Jerónimo"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );

        when(plotCommandService.handle(any(DelimitPlotCommand.class)))
                .thenReturn(Result.success(plot.snapshot().id().plotId()));
        when(plotRepository.findById(eq(plot.snapshot().id())))
                .thenReturn(Optional.of(plot));

        String payload = """
                {
                    "producerId": "550e8400-e29b-41d4-a716-446655440000",
                    "name": "Cuartel San Jerónimo",
                    "variety": "CRIOLLA",
                    "polygonGeoJson": "{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                    "rowSpacingM": 7.0,
                    "treeSpacingM": 5.0
                }
                """;

        mockMvc.perform(post("/api/v1/plots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(plot.snapshot().id().plotId())))
                .andExpect(jsonPath("$.name", is("Cuartel San Jerónimo")))
                .andExpect(jsonPath("$.variety", is("CRIOLLA")))
                .andExpect(jsonPath("$.areaHa", is(1.25)))
                .andExpect(jsonPath("$.treeDensity", is(286)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("POST /api/v1/plots should return 400 Bad Request with RFC 7807 ProblemDetail when name is too short")
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        String invalidPayload = """
                {
                    "producerId": "550e8400-e29b-41d4-a716-446655440000",
                    "name": "ab",
                    "variety": "CRIOLLA",
                    "polygonGeoJson": "{\\"type\\":\\"Polygon\\"}",
                    "rowSpacingM": 7.0,
                    "treeSpacingM": 5.0
                }
                """;

        mockMvc.perform(post("/api/v1/plots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("https://api.viora.com/errors/validation-error")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", containsString("Plot name must be between 3 and 100 characters")));
    }

    @Test
    @DisplayName("POST /api/v1/plots should return 400 Bad Request when producerId is blank")
    void shouldReturnBadRequestWhenProducerIdIsBlank() throws Exception {
        String payloadWithoutProducer = """
                {
                    "producerId": "",
                    "name": "Cuartel San Jerónimo",
                    "variety": "CRIOLLA",
                    "polygonGeoJson": "{\\"type\\":\\"Polygon\\",\\"coordinates\\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                    "rowSpacingM": 7.0,
                    "treeSpacingM": 5.0
                }
                """;

        mockMvc.perform(post("/api/v1/plots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithoutProducer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("https://api.viora.com/errors/validation-error")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", containsString("Producer id cannot be blank")));
    }

    @Test
    @DisplayName("POST /api/v1/plots should return 409 Conflict with RFC 7807 ProblemDetail on duplicate plot name in English")
    void shouldReturnConflictWhenPlotNameIsDuplicate() throws Exception {
        when(plotCommandService.handle(any(DelimitPlotCommand.class)))
                .thenReturn(Result.failure(ApplicationError.conflict("plot", "A plot with this name already exists for the producer")));

        String payload = """
                {
                    "producerId": "550e8400-e29b-41d4-a716-446655440000",
                    "name": "Cuartel Duplicado",
                    "variety": "CRIOLLA",
                    "polygonGeoJson": "{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                    "rowSpacingM": 7.0,
                    "treeSpacingM": 5.0
                }
                """;

        mockMvc.perform(post("/api/v1/plots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("https://api.viora.com/errors/plot-conflict")))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.detail", is("A plot with this name already exists for the producer")));
    }

    @Test
    @DisplayName("POST /api/v1/plots should return 409 Conflict with RFC 7807 ProblemDetail translated in Spanish when Accept-Language is es")
    void shouldReturnConflictTranslatedInSpanishWhenAcceptLanguageIsEs() throws Exception {
        when(plotCommandService.handle(any(DelimitPlotCommand.class)))
                .thenReturn(Result.failure(ApplicationError.conflict("plot", "Ya existe una parcela con este nombre para el productor")));

        String payload = """
                {
                    "producerId": "550e8400-e29b-41d4-a716-446655440000",
                    "name": "Cuartel Duplicado",
                    "variety": "CRIOLLA",
                    "polygonGeoJson": "{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                    "rowSpacingM": 7.0,
                    "treeSpacingM": 5.0
                }
                """;

        mockMvc.perform(post("/api/v1/plots")
                        .header("Accept-Language", "es")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("https://api.viora.com/errors/plot-conflict")))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.detail", is("Ya existe una parcela con este nombre para el productor")));
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId} should return 200 OK with ETag when revision matches")
    void shouldReturnOkAndETagWhenUpdatingPlotWithValidRevision() throws Exception {
        var plot = Plot.delimit(
                new ProducerId(producerId.toString()),
                new PlotName("Cuartel Rectificado"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(6.5, 4.5)
        );
        // simulate updated revision
        plot.update(
                new PlotName("Cuartel Rectificado"),
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(6.5, 4.5),
                null,
                0L
        );
        var plotId = UUID.fromString(plot.snapshot().id().plotId());

        when(plotCommandService.handle(any(UpdatePlotCommand.class)))
                .thenReturn(Result.success(plot));

        String updatePayload = """
                {
                    "name": "Cuartel Rectificado",
                    "rowSpacingM": 6.5,
                    "treeSpacingM": 4.5,
                    "lastPruningDate": "2026-09-10",
                    "polygonGeoJson": "{\\"type\\":\\"Polygon\\",\\"coordinates\\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}"
                }
                """;

        mockMvc.perform(put("/api/v1/plots/{plotId}", plotId)
                        .header("If-Match", "\"0\"")
                        .param("producerId", producerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(plot.snapshot().id().plotId())))
                .andExpect(jsonPath("$.name", is("Cuartel Rectificado")))
                .andExpect(jsonPath("$.rowSpacingM", is(6.5)))
                .andExpect(jsonPath("$.treeSpacingM", is(4.5)))
                .andExpect(jsonPath("$.revision", is(1)));
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId} should return 412 Precondition Failed when revision does not match")
    void shouldReturnPreconditionFailedWhenRevisionMismatch() throws Exception {
        var plotId = UUID.randomUUID();

        when(plotCommandService.handle(any(UpdatePlotCommand.class)))
                .thenReturn(Result.failure(ApplicationError.preconditionFailed("plot", "The plot revision has changed. Please reload.")));

        String updatePayload = """
                {
                    "name": "Cuartel Rectificado",
                    "rowSpacingM": 6.5,
                    "treeSpacingM": 4.5,
                    "lastPruningDate": null,
                    "polygonGeoJson": "{\\"type\\":\\"Polygon\\",\\"coordinates\\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}"
                }
                """;

        mockMvc.perform(put("/api/v1/plots/{plotId}", plotId)
                        .header("If-Match", "\"5\"")
                        .param("producerId", producerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.status", is(412)))
                .andExpect(jsonPath("$.type", is("https://api.viora.com/errors/plot-precondition-failed")));
    }

    @Test
    @DisplayName("PUT /api/v1/plots/{plotId} should return 400 Bad Request when If-Match header is malformed")
    void shouldReturnBadRequestWhenIfMatchIsMalformed() throws Exception {
        var plotId = UUID.randomUUID();

        String updatePayload = """
                {
                    "name": "Cuartel Rectificado",
                    "rowSpacingM": 6.5,
                    "treeSpacingM": 4.5,
                    "lastPruningDate": null,
                    "polygonGeoJson": "{\\"type\\":\\"Polygon\\",\\"coordinates\\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}"
                }
                """;

        mockMvc.perform(put("/api/v1/plots/{plotId}", plotId)
                        .header("If-Match", "invalid-revision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.type", is("https://api.viora.com/errors/validation-error")));
    }
}

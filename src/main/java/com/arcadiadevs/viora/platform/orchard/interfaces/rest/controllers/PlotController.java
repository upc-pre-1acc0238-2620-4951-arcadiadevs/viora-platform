package com.arcadiadevs.viora.platform.orchard.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.orchard.application.commandservices.PlotCommandService;
import com.arcadiadevs.viora.platform.orchard.application.queryservices.PlotQueryService;
import com.arcadiadevs.viora.platform.orchard.domain.exceptions.PlotNotFoundException;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetAllActivePlotsByProducerIdQuery;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetPlotByIdQuery;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.CreatePlotResource;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.PlotResource;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform.CreatePlotCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform.PlotResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * REST controller exposing endpoints for orchard plot management.
 */
@RestController
@RequestMapping(value = "/api/v1/plots", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Orchard Plots", description = "Endpoints for olive orchard plot delimitation and management")
@NullMarked
public class PlotController {

    private final PlotCommandService plotCommandService;
    private final PlotQueryService plotQueryService;
    private final PlotRepository plotRepository;

    /**
     * Constructor
     *
     * @param plotCommandService the plot command service
     * @param plotQueryService   the plot query service
     * @param plotRepository     the domain plot repository port
     */
    public PlotController(
            PlotCommandService plotCommandService,
            PlotQueryService plotQueryService,
            PlotRepository plotRepository
    ) {
        this.plotCommandService = plotCommandService;
        this.plotQueryService = plotQueryService;
        this.plotRepository = plotRepository;
    }

    /**
     * Delimits and registers a new orchard plot with georeferenced cadastral boundaries.
     *
     * @param resource the plot creation request payload
     * @return the created PlotResource with 201 Created, or ProblemDetail on failure
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Delimit and create georeferenced orchard plot",
            description = "Registers a new orchard plot, validates the cadastral polygon in WGS84, and derives planting density."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Plot successfully delimited and created",
                    content = @Content(schema = @Schema(implementation = PlotResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid payload or geometric validation error"),
            @ApiResponse(responseCode = "409", description = "A plot with the same name already exists for the producer")
    })
    public ResponseEntity<?> createPlot(@Valid @RequestBody CreatePlotResource resource) {
        var createPlotCommand = CreatePlotCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = plotCommandService.handle(createPlotCommand)
                .flatMap(plotId -> plotRepository.findById(new PlotId(plotId))
                        .<Result<Plot, ApplicationError>>map(Result::success)
                        .orElseGet(() -> Result.failure(ApplicationError.notFound("Plot", plotId))));

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                PlotResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED
        );
    }

    /**
     * Lists plots belonging to a producer with optional incremental delta synchronization.
     *
     * @param producerId   optional producer UUID parameter (falls back to default tenant producer if omitted)
     * @param updatedSince optional timestamp for delta synchronization
     * @return list of PlotResource objects with 200 OK
     */
    @GetMapping
    @Operation(
            summary = "List plots or delta synchronization",
            description = "Retrieves active orchard plots or increments modified since a timestamp for offline synchronization."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Plots successfully retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PlotResource.class)))
            ),
            @ApiResponse(responseCode = "400", description = "Malformed updatedSince date parameter format")
    })
    public ResponseEntity<List<PlotResource>> listPlots(
            @Parameter(description = "Managing producer UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) @Nullable String producerId,
            @Parameter(description = "Timestamp threshold for incremental delta sync (ISO-8601)", example = "2026-09-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Nullable Instant updatedSince
    ) {
        var effectiveProducerId = (producerId != null && !producerId.isBlank())
                ? new ProducerId(producerId)
                : new ProducerId("550e8400-e29b-41d4-a716-446655440000");

        var plots = (updatedSince != null)
                ? plotQueryService.handle(new GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery(effectiveProducerId, updatedSince))
                : plotQueryService.handle(new GetAllActivePlotsByProducerIdQuery(effectiveProducerId));

        return ResponseEntity.ok(PlotResourceFromEntityAssembler.toResourceList(plots));
    }

    /**
     * Retrieves the agronomic details and boundaries of an orchard plot by its unique identifier.
     *
     * @param plotId     the unique identifier of the plot
     * @param producerId optional producer UUID parameter (falls back to default tenant producer if omitted)
     * @return the PlotResource with 200 OK, or ProblemDetail with 404 Not Found if missing or inactive
     */
    @GetMapping("/{plotId}")
    @Operation(
            summary = "Get plot agronomic details by id",
            description = "Retrieves active orchard plot details, dendrometric spacing, calculated density, and cadastral geometry."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Plot details successfully retrieved",
                    content = @Content(schema = @Schema(implementation = PlotResource.class))
            ),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active for the producer")
    })
    public ResponseEntity<PlotResource> getPlotById(
            @Parameter(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable String plotId,
            @Parameter(description = "Managing producer UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) @Nullable String producerId
    ) {
        var effectiveProducerId = (producerId != null && !producerId.isBlank())
                ? new ProducerId(producerId)
                : new ProducerId("550e8400-e29b-41d4-a716-446655440000");

        var query = new GetPlotByIdQuery(new PlotId(plotId), effectiveProducerId);
        var plot = plotQueryService.handle(query)
                .orElseThrow(() -> new PlotNotFoundException(query.plotId()));

        return ResponseEntity.ok(PlotResourceFromEntityAssembler.toResourceFromEntity(plot));
    }
}


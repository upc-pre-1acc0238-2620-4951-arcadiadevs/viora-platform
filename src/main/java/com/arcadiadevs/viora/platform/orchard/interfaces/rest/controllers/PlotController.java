package com.arcadiadevs.viora.platform.orchard.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.orchard.application.commandservices.PlotCommandService;
import com.arcadiadevs.viora.platform.orchard.application.queryservices.PlotQueryService;
import com.arcadiadevs.viora.platform.orchard.domain.exceptions.PlotNotFoundException;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.RemovePlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetAllActivePlotsByProducerIdQuery;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetPlotByIdQuery;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetPlotsByProducerIdAndStatusQuery;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotStatus;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.CreatePlotResource;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.PlotResource;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.UpdatePlotResource;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform.CreatePlotCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform.PlotResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform.UpdatePlotCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.resources.MessageResource;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final String defaultProducerId;

    /**
     * Primary constructor for Spring injection.
     *
     * @param plotCommandService the plot command service
     * @param plotQueryService   the plot query service
     * @param plotRepository     the domain plot repository port
     * @param defaultProducerId  the default mock producer identifier from application properties
     */
    @Autowired
    public PlotController(
            PlotCommandService plotCommandService,
            PlotQueryService plotQueryService,
            PlotRepository plotRepository,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String defaultProducerId
    ) {
        this.plotCommandService = plotCommandService;
        this.plotQueryService = plotQueryService;
        this.plotRepository = plotRepository;
        this.defaultProducerId = defaultProducerId;
    }

    /**
     * Test-convenience constructor using default mock producer UUID.
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
        this(plotCommandService, plotQueryService, plotRepository, "550e8400-e29b-41d4-a716-446655440000");
    }

    private ProducerId resolveEffectiveProducerId() {
        return new ProducerId(defaultProducerId);
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
        var effectiveProducerId = resolveEffectiveProducerId();
        var createPlotCommand = CreatePlotCommandFromResourceAssembler.toCommandFromResource(
                effectiveProducerId.producerId(),
                resource
        );
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
     * Lists plots belonging to the authenticated producer with optional incremental delta synchronization
     * and an optional lifecycle status filter.
     *
     * @param updatedSince optional timestamp for delta synchronization
     * @param status       optional lifecycle status the plots must be in
     * @return list of PlotResource objects with 200 OK
     */
    @GetMapping
    @Operation(
            summary = "List plots or delta synchronization",
            description = "Retrieves the active orchard plots by default, or the plots in the given lifecycle status "
                    + "(REMOVED_SOFT_DELETE lists the archived ones), or the increments modified since a timestamp for "
                    + "offline synchronization (which includes removed plots, optionally narrowed by status)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Plots successfully retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PlotResource.class)))
            ),
            @ApiResponse(responseCode = "400", description = "Malformed updatedSince date or unknown status")
    })
    public ResponseEntity<List<PlotResource>> listPlots(
            @Parameter(description = "Timestamp threshold for incremental delta sync (ISO-8601)", example = "2026-09-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Nullable Instant updatedSince,
            @Parameter(description = "Lifecycle status the plots must be in; active plots when omitted",
                    schema = @Schema(allowableValues = {"ACTIVE", "REMOVED_SOFT_DELETE"}))
            @RequestParam(required = false) @Nullable PlotStatus status
    ) {
        var effectiveProducerId = resolveEffectiveProducerId();

        List<Plot> plots;
        if (updatedSince != null) {
            plots = plotQueryService.handle(new GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery(effectiveProducerId, updatedSince));
            if (status != null) {
                plots = plots.stream().filter(plot -> plot.snapshot().status() == status).toList();
            }
        } else if (status != null && status != PlotStatus.ACTIVE) {
            plots = plotQueryService.handle(new GetPlotsByProducerIdAndStatusQuery(effectiveProducerId, status));
        } else {
            plots = plotQueryService.handle(new GetAllActivePlotsByProducerIdQuery(effectiveProducerId));
        }

        return ResponseEntity.ok(PlotResourceFromEntityAssembler.toResourceList(plots));
    }

    /**
     * Retrieves the agronomic details and boundaries of an orchard plot by its unique identifier.
     *
     * @param plotId the unique identifier of the plot
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
            @PathVariable String plotId
    ) {
        var effectiveProducerId = resolveEffectiveProducerId();

        var query = new GetPlotByIdQuery(new PlotId(plotId), effectiveProducerId);
        var plot = plotQueryService.handle(query)
                .orElseThrow(() -> new PlotNotFoundException(query.plotId()));

        return ResponseEntity.ok(PlotResourceFromEntityAssembler.toResourceFromEntity(plot));
    }

    /**
     * Updates an orchard plot's boundaries and dendrometric frame with optimistic concurrency control.
     *
     * @param plotId   the identifier of the plot to update
     * @param ifMatch  the expected revision header (e.g., "1" or 1)
     * @param resource the payload containing updated boundaries and frame
     * @return updated PlotResource with ETag header and 200 OK, or ProblemDetail on error
     */
    @PutMapping(value = "/{plotId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Update plot boundaries and density with optimistic lock",
            description = "Updates plot boundaries, recalculates tree density, and enforces optimistic locking via the If-Match header."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Plot boundaries successfully updated",
                    content = @Content(schema = @Schema(implementation = PlotResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid payload or geometric boundary validation error"),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active for the producer"),
            @ApiResponse(responseCode = "409", description = "A plot with the same name already exists for the producer"),
            @ApiResponse(responseCode = "412", description = "Precondition Failed: If-Match revision mismatch")
    })
    public ResponseEntity<?> updatePlot(
            @Parameter(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable String plotId,
            @Parameter(description = "Optimistic locking revision", example = "\"1\"", required = true)
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody UpdatePlotResource resource
    ) {
        long expectedRevision;
        try {
            var rawRevision = ifMatch.replace("\"", "").trim();
            expectedRevision = Long.parseLong(rawRevision);
        } catch (NumberFormatException ex) {
            var error = ApplicationError.validationError("If-Match", "plot.revision.invalid");
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(error);
        }

        var effectiveProducerId = resolveEffectiveProducerId();

        var command = UpdatePlotCommandFromResourceAssembler
                .toCommandFromResource(plotId, effectiveProducerId.producerId(), expectedRevision, resource);

        var result = plotCommandService.handle(command);

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                PlotResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK
        );
    }

    /**
     * Soft deletes and deactivates an orchard plot from active inventory.
     *
     * @param plotId the identifier of the plot to delete
     * @param reason optional justification or cause for removing the plot
     * @return MessageResource with confirmation message and 200 OK, or ProblemDetail on error
     */
    @DeleteMapping("/{plotId}")
    @Operation(
            summary = "Soft delete plot from active inventory",
            description = "Removes an active orchard plot from inventory preserving historical traceability."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Plot deleted successfully",
                    content = @Content(schema = @Schema(implementation = MessageResource.class))
            ),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active for the producer"),
            @ApiResponse(responseCode = "409", description = "Plot has already been deactivated or removed")
    })
    public ResponseEntity<?> deletePlot(
            @Parameter(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true)
            @PathVariable String plotId,
            @Parameter(description = "Justification for removing the plot", example = "Manual plot removal")
            @RequestParam(required = false, defaultValue = "Manual plot removal") String reason
    ) {
        var effectiveProducerId = resolveEffectiveProducerId();

        var command = new RemovePlotCommand(plotId, effectiveProducerId.producerId(), reason);
        var result = plotCommandService.handle(command)
                .map(removedId -> new MessageResource("Plot deleted successfully"));

        return ResponseEntityAssembler.toResponseEntityFromResult(result, message -> message, HttpStatus.OK);
    }
}


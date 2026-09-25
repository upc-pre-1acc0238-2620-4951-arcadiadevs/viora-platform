package com.arcadiadevs.viora.platform.orchard.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.orchard.application.commandservices.PlotCommandService;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.CreatePlotResource;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.PlotResource;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform.CreatePlotCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform.PlotResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing endpoints for orchard plot management.
 */
@RestController
@RequestMapping(value = "/api/v1/plots", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Orchard Plots", description = "Endpoints for olive orchard plot delimitation and management")
@NullMarked
public class PlotController {

    private final PlotCommandService plotCommandService;
    private final PlotRepository plotRepository;

    /**
     * Constructor
     *
     * @param plotCommandService the plot command service
     * @param plotRepository     the domain plot repository port
     */
    public PlotController(PlotCommandService plotCommandService, PlotRepository plotRepository) {
        this.plotCommandService = plotCommandService;
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
}


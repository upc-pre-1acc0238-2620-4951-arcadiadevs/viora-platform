package com.arcadiadevs.viora.platform.phenology.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.phenology.application.commandservices.HarvestRecordCommandService;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.resources.HarvestRecordResource;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.resources.RecordHarvestYieldResource;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.transform.HarvestRecordResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.transform.RecordHarvestYieldCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.phenology.application.queryservices.HarvestRecordQueryService;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetHarvestRecordsByPlotIdQuery;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing endpoints for recording and managing annual campaign olive harvests.
 */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/harvest-records", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Harvest Records", description = "Endpoints for olive harvest memory and biennial bearing assessment")
@NullMarked
public class HarvestRecordController {

    private final HarvestRecordCommandService harvestRecordCommandService;
    private final HarvestRecordQueryService harvestRecordQueryService;
    private final ChillAccumulationTrackerRepository trackerRepository;

    /**
     * Constructs the controller injecting required services and repositories.
     *
     * @param harvestRecordCommandService the command service orchestrating harvest mutations
     * @param harvestRecordQueryService   the query service retrieving harvest entries
     * @param trackerRepository           the domain repository port
     */
    public HarvestRecordController(
            HarvestRecordCommandService harvestRecordCommandService,
            HarvestRecordQueryService harvestRecordQueryService,
            ChillAccumulationTrackerRepository trackerRepository
    ) {
        this.harvestRecordCommandService = harvestRecordCommandService;
        this.harvestRecordQueryService = harvestRecordQueryService;
        this.trackerRepository = trackerRepository;
    }

    /**
     * Records an annual harvest volume for an olive orchard plot, updating the Hoblyn BBI.
     *
     * @param plotId   the unique plot UUID string
     * @param resource the harvest registration payload
     * @return 201 Created with the registered HarvestRecordResource, or ProblemDetail on error
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Record annual campaign harvest yield",
            description = "Registers an annual harvest volume in kilograms and recalculates Hoblyn's Biennial Bearing Index (BBI)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Harvest yield successfully registered",
                    content = @Content(schema = @Schema(implementation = HarvestRecordResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid payload or incoherent yield sum"),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active in orchard context"),
            @ApiResponse(responseCode = "409", description = "A harvest record already exists for the campaign year on this plot")
    })
    public ResponseEntity<?> recordHarvestYield(
            @Parameter(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable String plotId,
            @Valid @RequestBody RecordHarvestYieldResource resource
    ) {
        var command = RecordHarvestYieldCommandFromResourceAssembler.toCommandFromResource(plotId, resource);
        var result = harvestRecordCommandService.handle(command)
                .flatMap(entryId -> trackerRepository.findByPlotId(new PlotId(plotId))
                        .<Result<HarvestRecordResource, ApplicationError>>map(tracker -> {
                            var snap = tracker.snapshot();
                            var entrySnapOpt = snap.harvestHistory().stream()
                                    .filter(e -> e.id().harvestEntryId().equals(entryId))
                                    .findFirst();

                            if (entrySnapOpt.isEmpty()) {
                                return Result.failure(ApplicationError.notFound("HarvestRecord", entryId));
                            }

                            var resourceOut = HarvestRecordResourceFromEntityAssembler.toResource(
                                    entrySnapOpt.get(),
                                    plotId,
                                    snap.calculatedBbi().value()
                                );
                            return Result.success(resourceOut);
                        })
                        .orElseGet(() -> Result.failure(ApplicationError.notFound("Plot", plotId))));

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                res -> res,
                HttpStatus.CREATED
        );
    }

    /**
     * Lists historical harvest records for an olive orchard plot, with optional campaign year filter.
     *
     * @param plotId       the unique plot UUID string
     * @param campaignYear optional campaign year filter
     * @return 200 OK with list of HarvestRecordResource, or ProblemDetail on error
     */
    @GetMapping
    @Operation(
            summary = "List historical harvest records for plot",
            description = "Retrieves the pluriannual harvest yield history and bearing classifications for an orchard plot, with optional campaign year filter."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Harvest records retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = HarvestRecordResource.class)))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid plot UUID format or campaign year range"),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active in orchard context")
    })
    public ResponseEntity<?> listHarvestRecords(
            @Parameter(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable String plotId,
            @Parameter(description = "Optional agricultural campaign year (1980 - 2100)", example = "2025")
            @RequestParam(required = false) @Nullable Integer campaignYear
    ) {
        var query = new GetHarvestRecordsByPlotIdQuery(plotId, campaignYear);
        var result = harvestRecordQueryService.handle(query)
                .map(entries -> {
                    var bbi = trackerRepository.findByPlotId(new PlotId(plotId))
                            .map(t -> t.snapshot().calculatedBbi().value())
                            .orElse(0.0);
                    return HarvestRecordResourceFromEntityAssembler.toResourceList(entries, plotId, bbi);
                });

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                res -> res,
                HttpStatus.OK
        );
    }
}

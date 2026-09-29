package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.thinning.application.commandservices.FruitThinningPrescriptionCommandService;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.SamplingSummaryResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.SubmitSamplingResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform.IngestFieldSamplingsBatchCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform.SamplingSummaryResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing endpoints for field sampling ingesting and statistical representativeness evaluation.
 */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/samplings", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Field Sampling", description = "Endpoints for guided olive shoot sampling and statistical coverage evaluation")
@NullMarked
public class FieldSamplingController {

    private final FruitThinningPrescriptionCommandService thinningCommandService;

    /**
     * Constructs the controller injecting the aggregate command service.
     *
     * @param thinningCommandService the command service orchestrating thinning prescription operations
     */
    public FieldSamplingController(FruitThinningPrescriptionCommandService thinningCommandService) {
        this.thinningCommandService = thinningCommandService;
    }

    /**
     * Submits a field sampling batch for an olive plot and campaign year.
     *
     * @param plotId   unique plot UUID
     * @param resource the batch submission payload resource
     * @return 201 Created with SamplingSummaryResource, or RFC 7807 ProblemDetail on failure
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Submit field sampling batch",
            description = "Ingests a batch of tree and shoot counts, deduplicating trees and checking statistical representativeness ($N \\ge 5$)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Sampling batch successfully ingested",
                    content = @Content(schema = @Schema(implementation = SamplingSummaryResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid payload, negative counts, or invalid date"),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active in orchard context"),
            @ApiResponse(responseCode = "409", description = "Duplicate tree tags within round or conflicting batch")
    })
    public ResponseEntity<?> submitSampling(
            @Parameter(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable String plotId,
            @Valid @RequestBody SubmitSamplingResource resource
    ) {
        var command = IngestFieldSamplingsBatchCommandFromResourceAssembler.toCommandFromResource(plotId, resource);
        var result = thinningCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                SamplingSummaryResourceFromEntityAssembler::toResource,
                HttpStatus.CREATED
        );
    }
}

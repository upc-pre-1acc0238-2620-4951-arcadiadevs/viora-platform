package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.thinning.application.internal.queryservices.ThinningPrescriptionOutlookService;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.application.commandservices.ConfirmThinningExecutionCommandService;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetThinningPrescriptionQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetThinningPrescriptionByIdQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionId;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.ConfirmExecutionResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.ExecutionConfirmationResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** REST entry point for confirming thinning field execution and reading it back. */
@RestController
@RequestMapping(value = "/api/v1/thinning-prescriptions/{id}/execution-confirmations",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Thinning Execution")
@PreAuthorize("@prescriptionAccess.isAccessible(#id)")
public class ThinningExecutionController {
    private final ConfirmThinningExecutionCommandService service;
    private final ThinningPrescriptionOutlookService outlookService;
    private final GetThinningPrescriptionQueryService queryService;

    public ThinningExecutionController(ConfirmThinningExecutionCommandService service,
            ThinningPrescriptionOutlookService outlookService, GetThinningPrescriptionQueryService queryService) {
        this.service = service;
        this.outlookService = outlookService;
        this.queryService = queryService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Confirm thinning execution",
            description = "Records prescribed fruit-thinning labor and evaluates biological timeliness. Late execution is accepted and marked LATE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Execution recorded",
                    content = @Content(schema = @Schema(implementation = ExecutionConfirmationResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID or execution data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The prescription belongs to another producer",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Prescription not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Prescription already confirmed, not prescribed, or missing its intervention window",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> confirm(@PathVariable String id, @Valid @RequestBody ConfirmExecutionResource resource) {
        var command = ConfirmThinningExecutionCommandFromResourceAssembler.toCommand(id, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(service.handle(command),
                prescription -> ExecutionConfirmationResourceFromEntityAssembler.toResource(prescription,
                        outlookService.yieldAfterThinning(prescription.snapshot())),
                HttpStatus.CREATED);
    }

    /**
     * Reads back the recorded labor with the same evidence the confirmation answered: the load it left, the caliber
     * projection issued then and the campaign yield. The app shows it again (P63) after the producer left the screen.
     *
     * @param id prescription UUID
     * @return the confirmation, or 404 while the labor was not recorded
     */
    @GetMapping
    @Operation(summary = "Get the recorded thinning execution",
            description = "Returns the execution confirmation of a prescription with its load balance, caliber projection and yield.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Execution retrieved",
                    content = @Content(schema = @Schema(implementation = ExecutionConfirmationResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid prescription UUID",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Prescription not found or its labor not recorded yet",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> get(@PathVariable String id) {
        Result<FruitThinningPrescription, ApplicationError> executed;
        try {
            executed = switch (queryService.handle(new GetThinningPrescriptionByIdQuery(new PrescriptionId(id)))) {
                case Result.Success<FruitThinningPrescriptionSnapshot, ApplicationError> success ->
                        success.value().executionConfirmation() == null
                                ? Result.failure(ApplicationError.notFound("ExecutionConfirmation", id))
                                : Result.success(FruitThinningPrescription.reconstitute(success.value()));
                case Result.Failure<FruitThinningPrescriptionSnapshot, ApplicationError> failure ->
                        Result.failure(failure.error());
            };
        } catch (IllegalArgumentException exception) {
            executed = Result.failure(ApplicationError.validationError("request", exception.getMessage()));
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(executed,
                prescription -> ExecutionConfirmationResourceFromEntityAssembler.toResource(prescription,
                        outlookService.yieldAfterThinning(prescription.snapshot())),
                HttpStatus.OK);
    }
}

package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.thinning.application.commandservices.ConfirmThinningExecutionCommandService;
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
import org.springframework.web.bind.annotation.*;

/** REST entry point for confirming thinning field execution. */
@RestController
@RequestMapping(value = "/api/v1/thinning-prescriptions/{id}/execution-confirmations",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Thinning Execution")
public class ThinningExecutionController {
    private final ConfirmThinningExecutionCommandService service;

    public ThinningExecutionController(ConfirmThinningExecutionCommandService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Confirm thinning execution",
            description = "Records prescribed fruit-thinning labor and evaluates biological timeliness. Late execution is accepted and marked LATE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Execution recorded",
                    content = @Content(schema = @Schema(implementation = ExecutionConfirmationResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID or execution data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Prescription not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Prescription already confirmed, not prescribed, or missing its intervention window",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> confirm(@PathVariable String id, @Valid @RequestBody ConfirmExecutionResource resource) {
        var command = ConfirmThinningExecutionCommandFromResourceAssembler.toCommand(id, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(service.handle(command),
                ExecutionConfirmationResourceFromEntityAssembler::toResource, HttpStatus.CREATED);
    }
}

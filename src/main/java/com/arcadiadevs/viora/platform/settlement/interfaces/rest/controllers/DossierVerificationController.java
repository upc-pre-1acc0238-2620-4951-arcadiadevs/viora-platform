package com.arcadiadevs.viora.platform.settlement.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.settlement.application.queryservices.SealedDossierQueryService;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.DossierVerificationResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform.AgronomicDossierResourceAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies the authenticity of a dossier PDF by its SHA-256 (US30 scenario 2). Anyone holding the file, such as a
 * bank or a cooperative, can check it: the code alone reveals only what the dossier already states.
 */
@RestController
@RequestMapping(value = "/api/v1/agronomic-dossiers/verifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Agronomic Dossier", description = "Dossier of a settled campaign, sealed by the producer with a SHA-256")
public class DossierVerificationController {

    private final SealedDossierQueryService queryService;

    public DossierVerificationController(SealedDossierQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{verificationHash}")
    @Operation(summary = "Verify a dossier by its SHA-256",
            description = "Answers 200 with the plot and campaign when the code matches a dossier stored by Viora, "
                    + "and 404 when it matches none (the file was changed or is not from Viora).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The code matches a stored dossier",
                    content = @Content(schema = @Schema(implementation = DossierVerificationResource.class))),
            @ApiResponse(responseCode = "400", description = "The code is not 64 hexadecimal characters",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No stored dossier has that code",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> verify(@PathVariable @Parameter(description = "SHA-256 of the PDF") String verificationHash) {
        return ResponseEntityAssembler.toResponseEntityFromResult(queryService.verify(verificationHash),
                AgronomicDossierResourceAssembler::toVerification, HttpStatus.OK);
    }
}

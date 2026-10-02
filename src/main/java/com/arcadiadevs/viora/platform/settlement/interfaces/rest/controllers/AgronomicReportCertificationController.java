package com.arcadiadevs.viora.platform.settlement.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.settlement.application.commandservices.CertifyAgronomicDossierCommandService;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.CertifyDossierResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.DossierCertificationResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform.CertifyAgronomicDossierCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform.DossierCertificationResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST entry point for certifying the dossier of one settled campaign of a plot. */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/certifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Agronomic Dossier Certification")
public class AgronomicReportCertificationController {
    private final CertifyAgronomicDossierCommandService service;

    /**
     * @param service command service certifying dossiers
     */
    public AgronomicReportCertificationController(CertifyAgronomicDossierCommandService service) {
        this.service = service;
    }

    /**
     * Certifies the dossier of a settled campaign.
     *
     * @param plotId   plot of the campaign
     * @param resource certification declaration
     * @return 201 with the stored certification, or the problem detail of the failure
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Certify agronomic dossier",
            description = "Certifies one settled campaign of the plot: renders its PDF dossier, stores the exact bytes "
                    + "with their SHA-256 verification hash and the declared collegiate signature, and publishes "
                    + "AgronomicDossierGeneratedEvent. A campaign can be certified once; campaigns whose frozen "
                    + "stabilization curve has insufficient settlements cannot be certified yet.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Campaign certified",
                    content = @Content(schema = @Schema(implementation = DossierCertificationResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid plot UUID, campaign, signature, name, CIP or notes",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Campaign already certified, certification race, or "
                    + "insufficient settlement history for its stabilization curve",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "The campaign has no official settlement",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "The PDF dossier could not be generated; nothing is stored",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> certify(@PathVariable String plotId, @Valid @RequestBody CertifyDossierResource resource) {
        var command = CertifyAgronomicDossierCommandFromResourceAssembler.toCommand(plotId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(service.handle(command),
                DossierCertificationResourceFromEntityAssembler::toResource, HttpStatus.CREATED);
    }
}

package com.arcadiadevs.viora.platform.settlement.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.settlement.application.commandservices.SealAgronomicDossierCommandService;
import com.arcadiadevs.viora.platform.settlement.application.queryservices.SealedDossierQueryService;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SealAgronomicDossierCommand;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.AgronomicDossierResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.SealDossierResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform.AgronomicDossierResourceAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Agronomic dossier sealed by the producer (US30): sealing it, reading its data and downloading its exact PDF.
 */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/agronomic-dossiers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Agronomic Dossier", description = "Dossier of a settled campaign, sealed by the producer with a SHA-256")
public class AgronomicDossierController {

    private final SealAgronomicDossierCommandService commandService;
    private final SealedDossierQueryService queryService;
    private final String actorId;

    /**
     * @param commandService seals dossiers
     * @param queryService   reads dossiers and their PDF
     * @param actorId        transitional actor until IAM: the configured mock producer
     */
    public AgronomicDossierController(SealAgronomicDossierCommandService commandService,
            SealedDossierQueryService queryService,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String actorId) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.actorId = actorId;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Seal the agronomic dossier of a settled campaign",
            description = "Gathers the harvest history, crop load, thinning, settled harvest and climate data of the "
                    + "campaign, renders the PDF in Spanish and stores its exact bytes with their SHA-256. Available "
                    + "from the first settled campaign. Sealing again returns the stored dossier (200) unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dossier sealed",
                    content = @Content(schema = @Schema(implementation = AgronomicDossierResource.class))),
            @ApiResponse(responseCode = "200", description = "The campaign already had its dossier; it is returned",
                    content = @Content(schema = @Schema(implementation = AgronomicDossierResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid plot UUID or campaign",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The plot belongs to another producer",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "The campaign is not settled",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "The PDF could not be rendered; nothing is stored",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> seal(@PathVariable String plotId, @Valid @RequestBody SealDossierResource resource) {
        var result = commandService.handle(new SealAgronomicDossierCommand(plotId, resource.campaignYear(), actorId));
        if (result instanceof Result.Success<SealAgronomicDossierCommandService.Sealing, ApplicationError> success) {
            var sealing = success.value();
            return new ResponseEntity<>(AgronomicDossierResourceAssembler.toResource(sealing.dossier()),
                    sealing.created() ? HttpStatus.CREATED : HttpStatus.OK);
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(result, sealing -> sealing, HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "List the sealed dossiers of a plot", description = "Most recent campaign first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossiers of the plot, empty when none was sealed",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AgronomicDossierResource.class)))),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> list(@PathVariable String plotId) {
        return ResponseEntityAssembler.toResponseEntityFromResult(queryService.findByPlot(plotId, actorId),
                dossiers -> dossiers.stream().map(AgronomicDossierResourceAssembler::toResource).toList(),
                HttpStatus.OK);
    }

    @GetMapping(value = "/{campaignYear}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PDF_VALUE})
    @Operation(summary = "Get the sealed dossier of a campaign",
            description = "With Accept: application/json returns its data and SHA-256; with Accept: application/pdf "
                    + "downloads the exact stored PDF (Content-Disposition attachment).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The dossier", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AgronomicDossierResource.class)),
                    @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                            schema = @Schema(type = "string", format = "binary"))}),
            @ApiResponse(responseCode = "404", description = "Plot not found or the campaign has no sealed dossier")
    })
    public ResponseEntity<?> get(@PathVariable String plotId,
            @PathVariable @Parameter(description = "Campaign year", example = "2026") Integer campaignYear,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept) {
        if (accept != null && accept.contains(MediaType.APPLICATION_PDF_VALUE)) {
            var file = queryService.findDocument(plotId, campaignYear, actorId);
            if (file instanceof Result.Success<SealedDossierQueryService.DossierFile, ApplicationError> success) {
                var dossier = success.value().dossier();
                byte[] bytes = success.value().document().content();
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .contentLength(bytes.length)
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                .filename(AgronomicDossierResourceAssembler.fileName(dossier), StandardCharsets.UTF_8)
                                .build().toString())
                        .header("X-Verification-Hash", dossier.verificationHash().value())
                        .body(bytes);
            }
            // A PDF client cannot read a JSON problem: the status says what happened
            var failure = (Result.Failure<SealedDossierQueryService.DossierFile, ApplicationError>) file;
            return ResponseEntity.status(ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error())
                    .getStatusCode()).build();
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(
                queryService.findByPlotAndCampaign(plotId, campaignYear, actorId),
                view -> AgronomicDossierResourceAssembler.toResource(view.dossier(), view.certification()),
                HttpStatus.OK);
    }
}

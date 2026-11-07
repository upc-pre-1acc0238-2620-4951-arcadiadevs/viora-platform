package com.arcadiadevs.viora.platform.territory.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.territory.application.queryservices.MemberPlotQueryService;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetMemberPlotByIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetMemberPlotsByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetProvisionalDossierByPlotIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetProvisionalDossierPdfByPlotIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.MemberPlotDetailResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.MemberPlotResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.ProvisionalDossierResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.transform.MemberPlotResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller of the plots of the members who share with a license, read-only, with the territorial traffic
 * light. It never exposes the producer's notes, assistant, payments nor location, and it never writes.
 */
@RestController
@RequestMapping(value = "/api/v1/member-plots", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Member Plots", description = "Plots of the members who share, with the territorial traffic light")
public class MemberPlotController {

    private final MemberPlotQueryService memberPlotQueryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the controller.
     *
     * @param memberPlotQueryService the member plot reads
     * @param currentUserProvider    the authenticated user
     * @throws IllegalArgumentException if any dependency is null
     */
    public MemberPlotController(MemberPlotQueryService memberPlotQueryService,
                                CurrentUserProvider currentUserProvider) {
        if (memberPlotQueryService == null || currentUserProvider == null) {
            throw new IllegalArgumentException("territory.member_plot_controller.dependency.null");
        }
        this.memberPlotQueryService = memberPlotQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Lists the plots of the members who share, in visit order.
     *
     * @param latitude  where the manager stands; optional, with longitude
     * @param longitude where the manager stands; optional, with latitude
     * @return 200 with the plots
     */
    @GetMapping
    @Operation(
            summary = "List the plots of the members who share",
            description = "Red first, then the larger overload, amber, grey and green; on a tie, the closest. With "
                    + "the manager's location each plot tells its distance; the location is not stored. Counts, "
                    + "sectors and «near you» are computed from this list.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The plots",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = MemberPlotResource.class)))),
            @ApiResponse(responseCode = "400", description = "Only one coordinate, or one out of range"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Not a manager with a license in force (reason "
                    + "territory.manager.no_license)")
    })
    public ResponseEntity<?> list(
            @Parameter(description = "Manager's latitude") @RequestParam(required = false) Double latitude,
            @Parameter(description = "Manager's longitude") @RequestParam(required = false) Double longitude) {
        final GetMemberPlotsByManagerIdQuery query;
        try {
            query = new GetMemberPlotsByManagerIdQuery(currentUserProvider.requireCurrentUserId(), latitude,
                    longitude);
        } catch (IllegalArgumentException ex) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.validationError("location", ex.getMessage()));
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(memberPlotQueryService.handle(query),
                plots -> plots.stream().map(MemberPlotResourceAssembler::toResource).toList(), HttpStatus.OK);
    }

    /**
     * Reads one plot of a member who shares.
     *
     * @param id the plot
     * @return 200 with the plot, or 404 if it is not a plot of a member who shares
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get a plot of a member who shares",
            description = "Load, sampling, prescription, thinning and the informative bearing index, with the "
                    + "owner's phone to coordinate a visit.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The plot",
                    content = @Content(schema = @Schema(implementation = MemberPlotDetailResource.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Not a manager with a license in force"),
            @ApiResponse(responseCode = "404", description = "Not a plot of a member who shares")
    })
    public ResponseEntity<?> get(@PathVariable UUID id) {
        var result = memberPlotQueryService.handle(new GetMemberPlotByIdAndManagerIdQuery(id.toString(),
                currentUserProvider.requireCurrentUserId()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result, MemberPlotResourceAssembler::toResource,
                HttpStatus.OK);
    }

    /**
     * Reads the provisional dossier of a plot of a member who shares, as it stands now.
     *
     * @param id the plot
     * @return 200 with the dossier, or 404 if it is not a plot of a member who shares
     */
    @GetMapping(value = "/{id}/provisional-dossier", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get the provisional dossier of a member plot",
            description = "Generated when asked, with the instant its data was read: provisional, not certified, "
                    + "without a verification code and not stored. Asking another day may give other values. The "
                    + "sealed dossier stays the producer's. Ask with Accept: application/pdf to download it.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The dossier",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ProvisionalDossierResource.class)),
                            @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                                    schema = @Schema(type = "string", format = "binary"))}),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Not a manager with a license in force"),
            @ApiResponse(responseCode = "404", description = "Not a plot of a member who shares")
    })
    public ResponseEntity<?> getProvisionalDossier(@PathVariable UUID id) {
        var result = memberPlotQueryService.handle(new GetProvisionalDossierByPlotIdAndManagerIdQuery(id.toString(),
                currentUserProvider.requireCurrentUserId()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result, MemberPlotResourceAssembler::toResource,
                HttpStatus.OK);
    }

    /**
     * Downloads the provisional dossier of a plot of a member who shares as a PDF.
     *
     * @param id the plot
     * @return 200 with the PDF as an attachment; on error only the status, since a PDF client reads no JSON
     */
    @GetMapping(value = "/{id}/provisional-dossier", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(hidden = true)
    public ResponseEntity<byte[]> downloadProvisionalDossier(@PathVariable UUID id) {
        var result = memberPlotQueryService.handle(new GetProvisionalDossierPdfByPlotIdAndManagerIdQuery(
                id.toString(), currentUserProvider.requireCurrentUserId()));
        if (result.isFailure()) {
            // A PDF client cannot read a JSON problem: the status says what happened
            return ResponseEntity.status(ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    result.failure().orElseThrow()).getStatusCode()).build();
        }
        var file = result.success().orElseThrow();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.content());
    }
}

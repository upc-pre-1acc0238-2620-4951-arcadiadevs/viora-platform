package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.RecordFullBloomResource;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.RecordFullBloomCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.EvaluateThinningPrescriptionCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.ThinningPrescriptionIssuer;
import com.arcadiadevs.viora.platform.thinning.application.commandservices.FruitThinningPrescriptionCommandService;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetThinningPrescriptionQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetActiveThinningPrescriptionQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.PrescriptionResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform.PrescriptionResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.time.ZoneOffset;
import java.util.Locale;

/**
 * REST controller exposing the current technical thinning prescription of an orchard plot.
 */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/thinning-prescriptions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Thinning Prescriptions", description = "Endpoints for retrieving technical fruit-thinning prescriptions")
@NullMarked
public class ThinningPrescriptionController {

    private static final Logger LOG = LoggerFactory.getLogger(ThinningPrescriptionController.class);

    private final GetThinningPrescriptionQueryService queryService;
    private final FruitThinningPrescriptionCommandService commandService;
    private final ThinningPrescriptionIssuer issuer;

    /**
     * Constructs the controller.
     *
     * @param queryService   read-only prescription query service
     * @param commandService command service that records the full bloom and issues prescriptions
     * @param issuer         tells what is still missing to issue a prescription
     */
    public ThinningPrescriptionController(
            GetThinningPrescriptionQueryService queryService,
            FruitThinningPrescriptionCommandService commandService,
            ThinningPrescriptionIssuer issuer
    ) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.issuer = issuer;
    }

    /**
     * Records the full bloom date observed on a plot, the origin of the thinning window, and issues the
     * prescription if that was the last missing input. Recording it again corrects it.
     *
     * @param plotId   plot UUID
     * @param resource the observed full bloom date
     * @return the prescription with its window and what is still missing, or an RFC 7807 error
     */
    @PutMapping(value = "/full-bloom", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Record the full bloom of a plot",
            description = "Stores the observed full bloom date of the campaign. The intervention window is counted from it using the technical profile of the plot variety."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Full bloom recorded",
                    content = @Content(schema = @Schema(implementation = PrescriptionResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid plot UUID, a future date or a date outside the campaign year"),
            @ApiResponse(responseCode = "404", description = "Plot does not exist or is inactive"),
            @ApiResponse(responseCode = "409", description = "The prescription is already confirmed or executed")
    })
    public ResponseEntity<?> recordFullBloom(
            @PathVariable @Parameter(description = "Unique plot UUID", required = true) String plotId,
            @Valid @RequestBody RecordFullBloomResource resource
    ) {
        int campaignYear = resource.campaignYear() != null ? resource.campaignYear() : resource.observedOn().getYear();
        final RecordFullBloomCommand command;
        try {
            command = new RecordFullBloomCommand(plotId, campaignYear, resource.observedOn());
        } catch (IllegalArgumentException exception) {
            return ResponseEntityAssembler.toResponseEntityFromResult(
                    Result.<FruitThinningPrescription, ApplicationError>failure(
                            ApplicationError.validationError("request", exception.getMessage())),
                    this::toResource,
                    HttpStatus.OK
            );
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(
                commandService.handle(command),
                this::toResource,
                HttpStatus.OK
        );
    }

    /**
     * Retrieves the active thinning prescription and its intervention window for an olive plot.
     *
     * @param plotId       plot UUID
     * @param campaignYear optional agricultural campaign year
     * @param status       optional lifecycle status filter; ACTIVE is treated as PRESCRIBED
     * @return prescription response or RFC 7807 validation/not-found error
     */
    @GetMapping
    @Operation(
            summary = "Get active thinning prescription",
            description = "Retrieves the thinning prescription of the plot and campaign in whatever state it is (SAMPLING_IN_PROGRESS while it cannot be issued, with the missing inputs in `blockers`). status=ACTIVE returns only the issued one."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Thinning prescription successfully retrieved",
                    content = @Content(schema = @Schema(implementation = PrescriptionResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid plot UUID, campaign year, or status filter"),
            @ApiResponse(responseCode = "404", description = "Plot does not exist or is inactive, or no prescription exists for the campaign")
    })
    public ResponseEntity<?> getActiveThinningPrescription(
            @PathVariable @Parameter(description = "Unique plot UUID", required = true) String plotId,
            @RequestParam(name = "campaignYear", required = false) @Nullable Integer campaignYear,
            @RequestParam(name = "status", required = false) @Nullable String status
    ) {
        final PlotId targetPlotId;
        final CampaignYear targetCampaignYear;
        final PrescriptionStatus statusFilter;

        try {
            targetPlotId = new PlotId(plotId);
            targetCampaignYear = campaignYear == null
                    ? new CampaignYear(Year.now(ZoneOffset.UTC).getValue())
                    : new CampaignYear(campaignYear);
            statusFilter = parseStatus(status);
        } catch (IllegalArgumentException exception) {
            return ResponseEntityAssembler.toResponseEntityFromResult(
                    Result.<FruitThinningPrescription, ApplicationError>failure(
                            ApplicationError.validationError("request", exception.getMessage())),
                    this::toResource,
                    HttpStatus.OK
            );
        }

        // A profile approved or a full bloom recorded after the sampling takes effect on the next read.
        // It is best effort: a concurrent read that issues it first must not make this one fail.
        try {
            commandService.handle(new EvaluateThinningPrescriptionCommand(plotId, targetCampaignYear.value()));
        } catch (RuntimeException exception) {
            LOG.warn("Thinning prescription of plot {} could not be evaluated on read: {}", plotId, exception.getMessage());
        }

        var query = new GetActiveThinningPrescriptionQuery(targetPlotId, targetCampaignYear, statusFilter);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                queryService.handle(query),
                snapshot -> PrescriptionResourceFromEntityAssembler.toResource(
                        snapshot, issuer.blockers(FruitThinningPrescription.reconstitute(snapshot))),
                HttpStatus.OK
        );
    }

    private PrescriptionResource toResource(FruitThinningPrescription prescription) {
        return PrescriptionResourceFromEntityAssembler.toResource(prescription.snapshot(), issuer.blockers(prescription));
    }

    /** No filter shows the prescription in whatever state it is; ACTIVE means only the issued (PRESCRIBED) one. */
    private @Nullable PrescriptionStatus parseStatus(@Nullable String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("ACTIVE".equals(normalized)) {
            return PrescriptionStatus.PRESCRIBED;
        }
        try {
            return PrescriptionStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("thinning.prescription.status.invalid");
        }
    }
}

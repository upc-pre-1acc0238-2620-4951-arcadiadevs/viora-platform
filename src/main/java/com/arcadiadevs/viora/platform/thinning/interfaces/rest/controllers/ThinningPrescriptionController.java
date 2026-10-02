package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

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

    private final GetThinningPrescriptionQueryService queryService;

    /**
     * Constructs the controller.
     *
     * @param queryService read-only prescription query service
     */
    public ThinningPrescriptionController(GetThinningPrescriptionQueryService queryService) {
        this.queryService = queryService;
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
            description = "Retrieves current active thinning prescription and phenological pit-hardening window for an olive plot."
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
                    Result.failure(ApplicationError.validationError("request", exception.getMessage())),
                    PrescriptionResourceFromEntityAssembler::toResource,
                    HttpStatus.OK
            );
        }

        var query = new GetActiveThinningPrescriptionQuery(targetPlotId, targetCampaignYear, statusFilter);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                queryService.handle(query),
                PrescriptionResourceFromEntityAssembler::toResource,
                HttpStatus.OK
        );
    }

    private PrescriptionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return PrescriptionStatus.PRESCRIBED;
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

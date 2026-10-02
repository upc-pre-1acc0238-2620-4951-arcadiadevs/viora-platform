package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.thinning.application.commandservices.FruitThinningPrescriptionCommandService;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetSamplingSummaryQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetSamplingSummaryByPlotIdQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.SamplingDetailedResource;
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
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.time.ZoneOffset;
import java.util.Locale;

/**
 * REST controller exposing endpoints for field sampling ingesting and statistical representativeness evaluation.
 */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/samplings", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Field Sampling", description = "Endpoints for guided olive shoot sampling and statistical coverage evaluation")
@NullMarked
public class FieldSamplingController {

    private final FruitThinningPrescriptionCommandService thinningCommandService;
    private final GetSamplingSummaryQueryService samplingSummaryQueryService;
    private final String defaultActorId;

    /**
     * Primary constructor injecting command service and configured fallback actor identifier.
     *
     * @param thinningCommandService the command service orchestrating thinning prescription operations
     * @param defaultActorId         the default mock user/actor identifier from application properties
     */
    @Autowired
    public FieldSamplingController(
            FruitThinningPrescriptionCommandService thinningCommandService,
            GetSamplingSummaryQueryService samplingSummaryQueryService,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String defaultActorId
    ) {
        this.thinningCommandService = thinningCommandService;
        this.samplingSummaryQueryService = samplingSummaryQueryService;
        this.defaultActorId = defaultActorId;
    }

    /**
     * Test-convenience constructor using the default mock actor identifier.
     *
     * @param thinningCommandService      command service for sampling ingestion
     * @param samplingSummaryQueryService query service for sampling summary
     */
    public FieldSamplingController(
            FruitThinningPrescriptionCommandService thinningCommandService,
            GetSamplingSummaryQueryService samplingSummaryQueryService
    ) {
        this(thinningCommandService, samplingSummaryQueryService, "550e8400-e29b-41d4-a716-446655440000");
    }

    /**
     * Test-convenience constructor using default actor identifier.
     *
     * @param thinningCommandService the command service orchestrating thinning prescription operations
     */
    public FieldSamplingController(FruitThinningPrescriptionCommandService thinningCommandService) {
        this(thinningCommandService, null, "550e8400-e29b-41d4-a716-446655440000");
    }

    /**
     * Resolves the current authenticated user/actor identifier.
     *
     * @return the resolved user UUID string
     */
    private String resolveEffectiveActorId() {
        return defaultActorId;
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
            description = "Ingests a batch of tree and shoot counts, deduplicating trees and checking statistical representativeness (minimum 5 trees evaluated)."
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
        String actorId = resolveEffectiveActorId();
        var command = IngestFieldSamplingsBatchCommandFromResourceAssembler.toCommandFromResource(plotId, actorId, resource);
        var result = thinningCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                SamplingSummaryResourceFromEntityAssembler::toResource,
                HttpStatus.CREATED
        );
    }

    /**
     * Retrieves the sampling statistical summary or detailed tree observations for a plot and campaign.
     *
     * @param plotId       unique plot UUID
     * @param campaignYear optional campaign year; defaults to current UTC calendar year
     * @param view         response view, either summary or detailed
     * @return 200 OK with the requested sampling view, or RFC 7807 error
     */
    @GetMapping
    @Operation(
            summary = "Get field sampling summary",
            description = "Retrieves statistical representativeness, unique tree count, and shoot density for an olive plot and campaign (minimum 5 trees evaluated). Use view=detailed to include per-tree sampling observations."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Field sampling summary successfully retrieved",
                    content = @Content(schema = @Schema(oneOf = {
                            SamplingSummaryResource.class,
                            SamplingDetailedResource.class
                    }))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid plot UUID, campaign year, or view selector"),
            @ApiResponse(responseCode = "404", description = "Plot not found or inactive in Orchard")
    })
    public ResponseEntity<?> getSamplingSummary(
            @Parameter(description = "Unique plot UUID", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable String plotId,
            @RequestParam(name = "campaignYear", required = false) @Nullable Integer campaignYear,
            @RequestParam(name = "view", defaultValue = "summary") String view
    ) {
        final PlotId targetPlotId;
        final CampaignYear targetCampaignYear;
        try {
            targetPlotId = new PlotId(plotId);
            targetCampaignYear = campaignYear == null
                    ? new CampaignYear(Year.now(ZoneOffset.UTC).getValue())
                    : new CampaignYear(campaignYear);
        } catch (IllegalArgumentException exception) {
            return ResponseEntityAssembler.toResponseEntityFromResult(
                    Result.failure(ApplicationError.validationError("request", exception.getMessage())),
                    SamplingSummaryResourceFromEntityAssembler::toResourceFromDomain,
                    HttpStatus.OK
            );
        }

        String normalizedView = view == null ? "" : view.trim().toLowerCase(Locale.ROOT);
        var query = new GetSamplingSummaryByPlotIdQuery(targetPlotId, targetCampaignYear);

        return switch (normalizedView) {
            case "summary" -> ResponseEntityAssembler.toResponseEntityFromResult(
                    samplingSummaryQueryService.handle(query),
                    SamplingSummaryResourceFromEntityAssembler::toResourceFromDomain,
                    HttpStatus.OK
            );
            case "detailed" -> ResponseEntityAssembler.toResponseEntityFromResult(
                    samplingSummaryQueryService.handleDetailed(query),
                    SamplingSummaryResourceFromEntityAssembler::toDetailedResource,
                    HttpStatus.OK
            );
            default -> ResponseEntityAssembler.toResponseEntityFromResult(
                    Result.failure(ApplicationError.validationError("view", "thinning.sampling_view.invalid")),
                    SamplingSummaryResourceFromEntityAssembler::toResourceFromDomain,
                    HttpStatus.OK
            );
        };
    }
}

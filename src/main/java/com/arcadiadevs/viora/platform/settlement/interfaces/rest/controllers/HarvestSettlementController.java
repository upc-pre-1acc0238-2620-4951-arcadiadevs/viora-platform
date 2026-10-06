package com.arcadiadevs.viora.platform.settlement.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.settlement.application.commandservices.HarvestSettlementCommandService;
import com.arcadiadevs.viora.platform.settlement.application.commandservices.SettlementOutcome;
import com.arcadiadevs.viora.platform.settlement.application.queryservices.HarvestSettlementQueryService;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetHarvestSettlementByPlotIdAndCampaignYearQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetHarvestSettlementsByPlotIdQuery;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.HarvestSettlementConflictResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.HarvestSettlementResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.SettleHarvestResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform.HarvestSettlementResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform.SettleCampaignHarvestCommandFromResourceAssembler;
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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST entry point for settling and consulting the annual harvest of a plot. */
@NullMarked
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/harvest-settlements", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Harvest Settlement")
public class HarvestSettlementController {
    private final HarvestSettlementCommandService service;
    private final HarvestSettlementQueryService queryService;
    private final HarvestSettlementResourceFromEntityAssembler assembler;
    private final String actorId;

    /**
     * @param service      command service
     * @param queryService query service
     * @param assembler    mapper of the settlement voucher to its response
     * @param actorId      transitional actor until IAM: the configured mock producer
     */
    public HarvestSettlementController(HarvestSettlementCommandService service,
            HarvestSettlementQueryService queryService,
            HarvestSettlementResourceFromEntityAssembler assembler,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String actorId) {
        this.service = service;
        this.queryService = queryService;
        this.assembler = assembler;
        this.actorId = actorId;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Settle campaign harvest",
            description = "Records the official green/black weighing of a campaign once, allocates its receipt number "
                    + "per producer and campaign, freezes its balance against the thinning prescription and its "
                    + "stabilization curve, and publishes CampaignHarvestSettledEvent. Sending the same "
                    + "Idempotency-Key again for the same plot and campaign replays the settlement instead of "
                    + "creating a second one.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Campaign settled",
                    content = @Content(schema = @Schema(implementation = HarvestSettlementResource.class))),
            @ApiResponse(responseCode = "200", description = "Campaign already settled under this idempotency key; "
                    + "the stored settlement is replayed and no new receipt number is consumed",
                    content = @Content(schema = @Schema(implementation = HarvestSettlementResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID or campaign, invalid weights or caliber, a "
                    + "missing or future weighing date, or an overlong mill ticket or Idempotency-Key header",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The actor does not own the plot",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Campaign already settled. The problem detail carries the "
                    + "code HARVESTSETTLEMENT_CONFLICT and an existingSettlement object with the campaignYear, "
                    + "totalYieldKg, receiptNumber and weighedOn of the settlement that is in place, so the app can "
                    + "reconcile without asking again.",
                    content = @Content(schema = @Schema(implementation = HarvestSettlementConflictResource.class))),
            @ApiResponse(responseCode = "422", description = "The Idempotency-Key is already in use for another plot or "
                    + "another campaign of the same producer. The problem detail carries the code "
                    + "BUSINESS_RULE_VIOLATION and no extra properties.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> settle(@PathVariable String plotId,
            @Parameter(description = "Optional key making the settlement replayable, up to 64 characters. Repeating it "
                    + "for the same plot and campaign returns the stored settlement with 200 instead of creating a "
                    + "second one; reusing it for another plot or campaign is rejected with 422.",
                    example = "8f4c1f2e-6c1a-4d5b-9a3e-2b7c9d0e1f55")
            @RequestHeader(name = "Idempotency-Key", required = false) @Nullable String idempotencyKey,
            @Valid @RequestBody SettleHarvestResource resource) {
        var command = SettleCampaignHarvestCommandFromResourceAssembler.toCommand(plotId, actorId, idempotencyKey,
                resource);
        return switch (service.handle(command)) {
            // A replay is not a creation, so it answers 200 while a fresh settlement answers 201.
            case Result.Success<SettlementOutcome, ApplicationError> success -> ResponseEntity
                    .status(success.value().created() ? HttpStatus.CREATED : HttpStatus.OK)
                    .body(assembler.toResource(success.value().settlement()));
            case Result.Failure<SettlementOutcome, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    /**
     * Lists the settled campaigns of a plot, newest campaign first.
     *
     * @param plotId the unique plot UUID string
     * @return 200 OK with the list of HarvestSettlementResource, empty when no campaign is settled yet, or
     *         ProblemDetail on error
     */
    @GetMapping
    @Operation(summary = "List campaign settlements of a plot",
            description = "Returns every settlement of the plot, newest campaign first, with its frozen thinning "
                    + "balance and stabilization curve. A plot that has never closed a campaign answers with an "
                    + "empty list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settlements listed",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = HarvestSettlementResource.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid plot UUID",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The actor does not own the plot",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> list(
            @Parameter(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable String plotId) {
        var query = new GetHarvestSettlementsByPlotIdQuery(plotId, actorId);
        var result = queryService.handle(query)
                .map(settlements -> settlements.stream()
                        .map(assembler::toResource)
                        .toList());
        return ResponseEntityAssembler.toResponseEntityFromResult(result, res -> res, HttpStatus.OK);
    }

    /**
     * Returns the settlement of one campaign of a plot.
     *
     * @param plotId       the unique plot UUID string
     * @param campaignYear the settled campaign, 2000 to 2100
     * @return 200 OK with the HarvestSettlementResource of the campaign, or ProblemDetail on error
     */
    @GetMapping("/{campaignYear}")
    @Operation(summary = "Get the settlement of one campaign",
            description = "Returns the frozen settlement of a single campaign of the plot, as registered by "
                    + "POST /harvest-settlements.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settlement found",
                    content = @Content(schema = @Schema(implementation = HarvestSettlementResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid plot UUID or campaign year",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The actor does not own the plot",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active, or campaign not settled",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> detail(
            @Parameter(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable String plotId,
            @Parameter(description = "Settled agricultural campaign year (2000 - 2100)", example = "2026")
            @PathVariable Integer campaignYear) {
        var query = new GetHarvestSettlementByPlotIdAndCampaignYearQuery(plotId, campaignYear, actorId);
        return ResponseEntityAssembler.toResponseEntityFromResult(queryService.handle(query),
                assembler::toResource, HttpStatus.OK);
    }
}

package com.arcadiadevs.viora.platform.settlement.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.settlement.application.commandservices.HarvestSettlementCommandService;
import com.arcadiadevs.viora.platform.settlement.application.queryservices.HarvestSettlementQueryService;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetHarvestSettlementByPlotIdAndCampaignYearQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetHarvestSettlementsByPlotIdQuery;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.HarvestSettlementResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.SettleHarvestResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform.HarvestSettlementResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform.SettleCampaignHarvestCommandFromResourceAssembler;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST entry point for settling and consulting the annual harvest of a plot. */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/harvest-settlements", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Harvest Settlement")
public class HarvestSettlementController {
    private final HarvestSettlementCommandService service;
    private final HarvestSettlementQueryService queryService;
    private final String actorId;

    /**
     * @param service      command service
     * @param queryService query service
     * @param actorId      transitional actor until IAM: the configured mock producer
     */
    public HarvestSettlementController(HarvestSettlementCommandService service,
            HarvestSettlementQueryService queryService,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String actorId) {
        this.service = service;
        this.queryService = queryService;
        this.actorId = actorId;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Settle campaign harvest",
            description = "Records the official green/black weighing of a campaign once, freezes its balance against "
                    + "the thinning prescription and its stabilization curve, and publishes CampaignHarvestSettledEvent.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Campaign settled",
                    content = @Content(schema = @Schema(implementation = HarvestSettlementResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID, campaign, weights or caliber",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The actor does not own the plot",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Campaign already settled",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> settle(@PathVariable String plotId, @Valid @RequestBody SettleHarvestResource resource) {
        var command = SettleCampaignHarvestCommandFromResourceAssembler.toCommand(plotId, actorId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(service.handle(command),
                HarvestSettlementResourceFromEntityAssembler::toResource, HttpStatus.CREATED);
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
                        .map(HarvestSettlementResourceFromEntityAssembler::toResource)
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
                HarvestSettlementResourceFromEntityAssembler::toResource, HttpStatus.OK);
    }
}

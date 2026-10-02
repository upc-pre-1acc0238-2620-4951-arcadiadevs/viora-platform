package com.arcadiadevs.viora.platform.settlement.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.settlement.application.commandservices.HarvestSettlementCommandService;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.HarvestSettlementResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.SettleHarvestResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform.HarvestSettlementResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform.SettleCampaignHarvestCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
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

/** REST entry point for settling the annual harvest of a plot. */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/harvest-settlements", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Harvest Settlement")
public class HarvestSettlementController {
    private final HarvestSettlementCommandService service;
    private final String actorId;

    /**
     * @param service command service
     * @param actorId transitional actor until IAM: the configured mock producer
     */
    public HarvestSettlementController(HarvestSettlementCommandService service,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String actorId) {
        this.service = service;
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
}

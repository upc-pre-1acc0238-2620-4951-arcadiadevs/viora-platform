package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetThinningEventsQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetThinningEventsQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.ThinningEventsResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform.ThinningEventsResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.time.ZoneOffset;

/**
 * REST controller exposing agronomic milestone events from the thinning bounded context for the logbook feed.
 */
@RestController
@RequestMapping(value = "/api/v1/thinning-events", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Thinning Events", description = "Endpoints for retrieving fruit thinning lifecycle events for the field logbook")
@NullMarked
public class ThinningEventController {

    private final GetThinningEventsQueryService queryService;
    private final String defaultActorId;

    /**
     * Primary constructor injecting query service and default actor identifier.
     *
     * @param queryService   the query service retrieving thinning events
     * @param defaultActorId default mock producer/actor identifier
     */
    @Autowired
    public ThinningEventController(
            GetThinningEventsQueryService queryService,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String defaultActorId
    ) {
        this.queryService = queryService;
        this.defaultActorId = defaultActorId;
    }

    /**
     * Test-convenience constructor using default actor identifier.
     *
     * @param queryService query service retrieving thinning events
     */
    public ThinningEventController(GetThinningEventsQueryService queryService) {
        this(queryService, "550e8400-e29b-41d4-a716-446655440000");
    }

    /**
     * Retrieves thinning lifecycle events for the producer, filtered optionally by campaign year and plot.
     *
     * @param campaignYear optional agricultural campaign year (defaults to current UTC calendar year)
     * @param plotId       optional plot identifier filter UUID
     * @return 200 OK with ThinningEventsResource, or RFC 7807 error
     */
    @GetMapping
    @Operation(
            summary = "Get thinning lifecycle events",
            description = "Retrieves completed sampling rounds and executed thinning operations as chronological events for the field logbook."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Thinning events successfully retrieved",
                    content = @Content(schema = @Schema(implementation = ThinningEventsResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid UUID format or campaign year"),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active in orchard context")
    })
    public ResponseEntity<?> getThinningEvents(
            @Parameter(description = "Agricultural campaign year", example = "2026")
            @RequestParam(name = "campaignYear", required = false) @Nullable Integer campaignYear,
            @Parameter(description = "Optional unique plot UUID filter", example = "550e8400-e29b-41d4-a716-446655440001")
            @RequestParam(name = "plotId", required = false) @Nullable String plotId
    ) {
        final CampaignYear targetCampaignYear;
        final PlotId targetPlotId;
        try {
            targetCampaignYear = campaignYear == null
                    ? new CampaignYear(Year.now(ZoneOffset.UTC).getValue())
                    : new CampaignYear(campaignYear);
            targetPlotId = (plotId == null || plotId.isBlank()) ? null : new PlotId(plotId);
        } catch (IllegalArgumentException exception) {
            return ResponseEntityAssembler.toResponseEntityFromResult(
                    Result.failure(ApplicationError.validationError("request", exception.getMessage())),
                    ThinningEventsResourceAssembler::toResourceFromDomain,
                    HttpStatus.OK
            );
        }

        var query = new GetThinningEventsQuery(defaultActorId, targetCampaignYear, targetPlotId);
        var result = queryService.handle(query);

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                ThinningEventsResourceAssembler::toResourceFromDomain,
                HttpStatus.OK
        );
    }
}

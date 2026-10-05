package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetPlotSamplingStatesQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetPlotSamplingStatesQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.PlotSamplingStateResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform.PlotSamplingStateResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
 * REST controller exposing endpoints for the global collection of field samplings across all producer plots.
 */
@RestController
@RequestMapping(value = "/api/v1/samplings", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Field Sampling", description = "Endpoints for guided olive shoot sampling and statistical coverage evaluation")
@NullMarked
public class PlotSamplingCollectionController {

    private final GetPlotSamplingStatesQueryService queryService;
    private final String defaultProducerId;

    /**
     * Primary constructor injecting query service and default producer identifier.
     *
     * @param queryService      query service retrieving plot sampling states
     * @param defaultProducerId fallback producer identifier from configuration
     */
    @Autowired
    public PlotSamplingCollectionController(
            GetPlotSamplingStatesQueryService queryService,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String defaultProducerId
    ) {
        this.queryService = queryService;
        this.defaultProducerId = defaultProducerId;
    }

    /**
     * Test-convenience constructor using default producer identifier.
     *
     * @param queryService query service retrieving plot sampling states
     */
    public PlotSamplingCollectionController(GetPlotSamplingStatesQueryService queryService) {
        this(queryService, "550e8400-e29b-41d4-a716-446655440000");
    }

    /**
     * Retrieves the sampling states of all active plots for the producer in the specified campaign.
     *
     * @param campaignYear optional agricultural campaign year (defaults to current UTC calendar year)
     * @return 200 OK with list of PlotSamplingStateResource, or RFC 7807 error
     */
    @GetMapping
    @Operation(
            summary = "Get plot sampling states collection",
            description = "Retrieves sampling progress, coverage state, and trees needed across all producer plots for the plot picker screen."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Plot sampling states successfully retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PlotSamplingStateResource.class)))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid campaign year")
    })
    public ResponseEntity<?> getPlotSamplingStates(
            @Parameter(description = "Agricultural campaign year", example = "2026")
            @RequestParam(name = "campaignYear", required = false) @Nullable Integer campaignYear
    ) {
        final CampaignYear targetCampaignYear;
        try {
            targetCampaignYear = campaignYear == null
                    ? new CampaignYear(Year.now(ZoneOffset.UTC).getValue())
                    : new CampaignYear(campaignYear);
        } catch (IllegalArgumentException exception) {
            return ResponseEntityAssembler.toResponseEntityFromResult(
                    Result.failure(ApplicationError.validationError("campaignYear", exception.getMessage())),
                    PlotSamplingStateResourceAssembler::toResourceList,
                    HttpStatus.OK
            );
        }

        var query = new GetPlotSamplingStatesQuery(defaultProducerId, targetCampaignYear);
        var result = queryService.handle(query);

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                PlotSamplingStateResourceAssembler::toResourceList,
                HttpStatus.OK
        );
    }
}

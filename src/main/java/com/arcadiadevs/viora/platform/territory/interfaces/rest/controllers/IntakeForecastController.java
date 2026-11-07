package com.arcadiadevs.viora.platform.territory.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.territory.application.queryservices.IntakeForecastQueryService;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetIntakeForecastByCampaignYearAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetIntakeForecastsByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.IntakeForecastResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.transform.IntakeForecastResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller of the intake of the manager's license, read-only: the projection of the campaign in progress and
 * the intake settled in past ones, identified by their campaign year.
 */
@RestController
@RequestMapping(value = "/api/v1/intake-forecasts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Intake Forecasts", description = "Intake of the manager's license, projected or settled")
public class IntakeForecastController {

    private final IntakeForecastQueryService intakeForecastQueryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the controller.
     *
     * @param intakeForecastQueryService the intake reads
     * @param currentUserProvider        the authenticated user
     * @throws IllegalArgumentException if any dependency is null
     */
    public IntakeForecastController(IntakeForecastQueryService intakeForecastQueryService,
                                    CurrentUserProvider currentUserProvider) {
        if (intakeForecastQueryService == null || currentUserProvider == null) {
            throw new IllegalArgumentException("territory.intake_controller.dependency.null");
        }
        this.intakeForecastQueryService = intakeForecastQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Lists the campaigns of the license, newest first.
     *
     * @return 200 with the intake of each campaign, without sectors
     */
    @GetMapping
    @Operation(
            summary = "List the intake of each campaign",
            description = "The campaign in progress is projected from the estimable plots of the members who share; "
                    + "past ones add up the harvests settled since each member joined.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The campaigns",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = IntakeForecastResource.class)))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Not a manager with a license in force (reason "
                    + "territory.manager.no_license)")
    })
    public ResponseEntity<?> list() {
        var result = intakeForecastQueryService.handle(
                new GetIntakeForecastsByManagerIdQuery(currentUserProvider.requireCurrentUserId()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                intakes -> intakes.stream().map(IntakeForecastResourceAssembler::toResource).toList(), HttpStatus.OK);
    }

    /**
     * Reads the intake of one campaign, with its sectors.
     *
     * @param campaignYear the campaign
     * @return 200 with the intake, or 404 for a future campaign
     */
    @GetMapping("/{campaignYear}")
    @Operation(
            summary = "Get the intake of a campaign",
            description = "Only estimable plots add up, without extrapolation; green and black follow each plot's "
                    + "settled history, the rest is unclassified. Below 50 % sampling coverage the projection is "
                    + "preliminary, and without any sampling there is no figure.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The intake",
                    content = @Content(schema = @Schema(implementation = IntakeForecastResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid campaign year"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Not a manager with a license in force"),
            @ApiResponse(responseCode = "404", description = "A future campaign")
    })
    public ResponseEntity<?> get(@PathVariable int campaignYear) {
        final GetIntakeForecastByCampaignYearAndManagerIdQuery query;
        try {
            query = new GetIntakeForecastByCampaignYearAndManagerIdQuery(campaignYear,
                    currentUserProvider.requireCurrentUserId());
        } catch (IllegalArgumentException ex) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.validationError("campaignYear", ex.getMessage()));
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(intakeForecastQueryService.handle(query),
                IntakeForecastResourceAssembler::toResource, HttpStatus.OK);
    }
}

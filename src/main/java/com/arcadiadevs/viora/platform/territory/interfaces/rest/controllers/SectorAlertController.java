package com.arcadiadevs.viora.platform.territory.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.territory.application.queryservices.SectorAlertQueryService;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetSectorAlertsByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.SectorAlertResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.transform.SectorAlertResourceAssembler;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller of the sector alert aggregate: the manager's alert center, read-only. The alerts are kept up to
 * date by the platform every few minutes; the manager only reads them.
 */
@RestController
@RequestMapping(value = "/api/v1/sector-alerts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Sector Alerts", description = "Alerts about the sectors of the manager's license")
public class SectorAlertController {

    private final SectorAlertQueryService sectorAlertQueryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the controller.
     *
     * @param sectorAlertQueryService the alert reads
     * @param currentUserProvider     the authenticated user
     * @throws IllegalArgumentException if any dependency is null
     */
    public SectorAlertController(SectorAlertQueryService sectorAlertQueryService,
                                 CurrentUserProvider currentUserProvider) {
        if (sectorAlertQueryService == null || currentUserProvider == null) {
            throw new IllegalArgumentException("territory.sector_alert_controller.dependency.null");
        }
        this.sectorAlertQueryService = sectorAlertQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Lists the alerts of the signed-in manager's sectors.
     *
     * @return 200 with the active alerts, critical first, and those normalized in the last 7 days
     */
    @GetMapping
    @Operation(
            summary = "List the alerts of the license's sectors",
            description = "A sector turns critical when it has plots in red for overload or a critical incident, and "
                    + "gets a frost alert («Atención») when frost is forecast or open. Each alert names its leading "
                    + "plot while its owner shares, and normalizes once the cause is gone, telling how the sector was "
                    + "left. Alerts normalized more than 7 days ago are not listed.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The alerts",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = SectorAlertResource.class)))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Not a manager with a license in force (reason "
                    + "territory.manager.no_license)")
    })
    public ResponseEntity<?> list() {
        var result = sectorAlertQueryService.handle(
                new GetSectorAlertsByManagerIdQuery(currentUserProvider.requireCurrentUserId()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                alerts -> alerts.stream().map(SectorAlertResourceAssembler::toResource).toList(), HttpStatus.OK);
    }
}

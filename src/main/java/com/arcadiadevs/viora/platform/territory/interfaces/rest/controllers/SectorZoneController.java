package com.arcadiadevs.viora.platform.territory.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.territory.application.commandservices.SectorZoneCommandService;
import com.arcadiadevs.viora.platform.territory.application.queryservices.SectorZoneQueryService;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.CreateSectorZoneCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.RenameSectorZoneCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetSectorZoneByIdAndUserIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetSectorZonesByUserIdQuery;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.SectorZoneNameResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.SectorZoneResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.transform.SectorZoneResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller of the sector zone aggregate: the sector list of a cooperative license.
 * <p>The license always comes from the session: the manager reaches the one they are in charge of, and a member the
 * one that sponsors their plan. Only the manager adds and renames sectors.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/sector-zones", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Sector Zones", description = "Sector list of a cooperative license")
public class SectorZoneController {

    private final SectorZoneCommandService sectorZoneCommandService;
    private final SectorZoneQueryService sectorZoneQueryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the controller.
     *
     * @param sectorZoneCommandService the add and rename use cases
     * @param sectorZoneQueryService   the sector reads
     * @param currentUserProvider      the authenticated user
     * @throws IllegalArgumentException if any dependency is null
     */
    public SectorZoneController(SectorZoneCommandService sectorZoneCommandService,
                                SectorZoneQueryService sectorZoneQueryService,
                                CurrentUserProvider currentUserProvider) {
        if (sectorZoneCommandService == null || sectorZoneQueryService == null || currentUserProvider == null) {
            throw new IllegalArgumentException("territory.sector_zone_controller.dependency.null");
        }
        this.sectorZoneCommandService = sectorZoneCommandService;
        this.sectorZoneQueryService = sectorZoneQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Lists the sectors of the license the signed-in user reaches.
     *
     * @return 200 with the sectors, alphabetically
     */
    @GetMapping
    @Operation(
            summary = "List the sectors of the license",
            description = "A technical manager gets the list of the license they are in charge of; a member, the "
                    + "list of the license that sponsors their plan. Each sector counts its member plots.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The sectors",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = SectorZoneResource.class)))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "A manager without a license in force (reason "
                    + "territory.manager.no_license) or a producer without a sponsored plan (reason "
                    + "territory.producer.not_sponsored)")
    })
    public ResponseEntity<?> list() {
        var result = sectorZoneQueryService.handle(
                new GetSectorZonesByUserIdQuery(currentUserProvider.requireCurrentUserId()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                zones -> zones.stream().map(SectorZoneResourceAssembler::toResource).toList(), HttpStatus.OK);
    }

    /**
     * Reads one sector of the license the signed-in user reaches.
     *
     * @param id the sector
     * @return 200 with the sector, or 404 if it is not in that list
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a sector of the license", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The sector",
                    content = @Content(schema = @Schema(implementation = SectorZoneResource.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user reaches no license"),
            @ApiResponse(responseCode = "404", description = "No such sector in the license")
    })
    public ResponseEntity<?> get(@PathVariable UUID id) {
        var result = sectorZoneQueryService.handle(
                new GetSectorZoneByIdAndUserIdQuery(id.toString(), currentUserProvider.requireCurrentUserId()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result, SectorZoneResourceAssembler::toResource,
                HttpStatus.OK);
    }

    /**
     * Adds a sector to the signed-in manager's license.
     *
     * @param resource the name
     * @return 201 with the sector and its location, or a problem detail explaining why it was not added
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Add a sector",
            description = "Available at once to every member of the license. Accents and case do not make a "
                    + "different sector.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sector added",
                    content = @Content(schema = @Schema(implementation = SectorZoneResource.class))),
            @ApiResponse(responseCode = "400", description = "Blank, too short or too long name"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a manager with a license in force"),
            @ApiResponse(responseCode = "409", description = "The list already has that name (reason "
                    + "territory.sector_zone.name_taken)")
    })
    public ResponseEntity<?> create(@Valid @RequestBody SectorZoneNameResource resource) {
        var userId = currentUserProvider.requireCurrentUserId();
        var result = sectorZoneCommandService.handle(new CreateSectorZoneCommand(userId, resource.name()));
        if (result.isFailure()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(result.failure().orElseThrow());
        }
        var id = result.success().orElseThrow().snapshot().id().sectorZoneId();
        var view = sectorZoneQueryService.handle(new GetSectorZoneByIdAndUserIdQuery(id, userId));
        if (view.isFailure()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(view.failure().orElseThrow());
        }
        return ResponseEntity.created(URI.create("/api/v1/sector-zones/" + id))
                .body(SectorZoneResourceAssembler.toResource(view.success().orElseThrow()));
    }

    /**
     * Renames a sector of the signed-in manager's license.
     *
     * @param id       the sector
     * @param resource the new name
     * @return 200 with the sector, or a problem detail explaining why it was not renamed
     */
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Rename a sector",
            description = "Every plot in the sector shows the new name. Sectors are not deleted in the MVP.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sector renamed",
                    content = @Content(schema = @Schema(implementation = SectorZoneResource.class))),
            @ApiResponse(responseCode = "400", description = "Blank, too short or too long name"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a manager with a license in force"),
            @ApiResponse(responseCode = "404", description = "No such sector in the license"),
            @ApiResponse(responseCode = "409", description = "Another sector already has that name (reason "
                    + "territory.sector_zone.name_taken)")
    })
    public ResponseEntity<?> rename(@PathVariable UUID id, @Valid @RequestBody SectorZoneNameResource resource) {
        var userId = currentUserProvider.requireCurrentUserId();
        var result = sectorZoneCommandService.handle(
                new RenameSectorZoneCommand(userId, id.toString(), resource.name()));
        if (result.isFailure()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(result.failure().orElseThrow());
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(
                sectorZoneQueryService.handle(new GetSectorZoneByIdAndUserIdQuery(id.toString(), userId)),
                SectorZoneResourceAssembler::toResource, HttpStatus.OK);
    }
}

package com.arcadiadevs.viora.platform.territory.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.territory.application.queryservices.CooperativeMemberQueryService;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetCooperativeMemberByIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetCooperativeMembersByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.CooperativeMemberDetailResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.CooperativeMemberResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.transform.CooperativeMemberResourceAssembler;
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

import java.util.UUID;

/**
 * REST controller of the member roll of a cooperative license, read-only, as its technical manager sees it.
 * <p>The license comes from the session. A member who does not share, or stopped sharing, shows only their name,
 * state and plan.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/cooperative-members", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Cooperative Members", description = "Member roll of the manager's license, read-only")
public class CooperativeMemberController {

    private final CooperativeMemberQueryService cooperativeMemberQueryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the controller.
     *
     * @param cooperativeMemberQueryService the member reads
     * @param currentUserProvider           the authenticated user
     * @throws IllegalArgumentException if any dependency is null
     */
    public CooperativeMemberController(CooperativeMemberQueryService cooperativeMemberQueryService,
                                       CurrentUserProvider currentUserProvider) {
        if (cooperativeMemberQueryService == null || currentUserProvider == null) {
            throw new IllegalArgumentException("territory.member_controller.dependency.null");
        }
        this.cooperativeMemberQueryService = cooperativeMemberQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Lists the members of the signed-in manager's license.
     *
     * @return 200 with the members, by name
     */
    @GetMapping
    @Operation(
            summary = "List the members of the license",
            description = "Each member with whether they share; those who share also with their plots, hectares, "
                    + "sectors and their most urgent plot.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The members",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = CooperativeMemberResource.class)))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Not a manager with a license in force (reason "
                    + "territory.manager.no_license)")
    })
    public ResponseEntity<?> list() {
        var result = cooperativeMemberQueryService.handle(
                new GetCooperativeMembersByManagerIdQuery(currentUserProvider.requireCurrentUserId()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                members -> members.stream().map(CooperativeMemberResourceAssembler::toResource).toList(),
                HttpStatus.OK);
    }

    /**
     * Reads one member of the signed-in manager's license.
     *
     * @param id the member's user identifier
     * @return 200 with the member, or 404 if they are not a member of the license
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get a member of the license",
            description = "Phone, email and plots only while the member shares.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The member",
                    content = @Content(schema = @Schema(implementation = CooperativeMemberDetailResource.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Not a manager with a license in force"),
            @ApiResponse(responseCode = "404", description = "Not a member of the license")
    })
    public ResponseEntity<?> get(@PathVariable UUID id) {
        var result = cooperativeMemberQueryService.handle(new GetCooperativeMemberByIdAndManagerIdQuery(
                id.toString(), currentUserProvider.requireCurrentUserId()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                CooperativeMemberResourceAssembler::toResource, HttpStatus.OK);
    }
}

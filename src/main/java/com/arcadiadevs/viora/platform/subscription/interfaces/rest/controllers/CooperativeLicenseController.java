package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.CooperativeLicenseQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetAllCooperativeLicensesByManagerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeLicenseByIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.CooperativeLicenseResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.CooperativeLicenseResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller of the cooperative license aggregate, as its technical manager sees it.
 * <p>The manager always comes from the session: each one only reaches the license they are in charge of, whatever
 * organization holds it.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/cooperative-licenses", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Cooperative Licenses", description = "The license a technical manager is in charge of")
public class CooperativeLicenseController {

    private final CooperativeLicenseQueryService cooperativeLicenseQueryService;
    private final CooperativeLicenseResourceFromEntityAssembler cooperativeLicenseResourceAssembler;
    private final CurrentUserProvider users;

    /**
     * Creates the controller.
     *
     * @param cooperativeLicenseQueryService      the license reads
     * @param cooperativeLicenseResourceAssembler the assembler adding what each license has left
     * @param users                               the authenticated user
     * @throws IllegalArgumentException if any dependency is null
     */
    public CooperativeLicenseController(
            CooperativeLicenseQueryService cooperativeLicenseQueryService,
            CooperativeLicenseResourceFromEntityAssembler cooperativeLicenseResourceAssembler,
            CurrentUserProvider users) {
        if (cooperativeLicenseQueryService == null || cooperativeLicenseResourceAssembler == null || users == null) {
            throw new IllegalArgumentException("subscription.license_controller.dependency.null");
        }
        this.cooperativeLicenseQueryService = cooperativeLicenseQueryService;
        this.cooperativeLicenseResourceAssembler = cooperativeLicenseResourceAssembler;
        this.users = users;
    }

    /**
     * Lists the licenses the authenticated technical manager is in charge of.
     *
     * @return 200 with the licenses, in force or not; empty while the manager has not redeemed a code
     */
    @GetMapping
    @Operation(
            summary = "List the licenses of the signed-in technical manager",
            description = "A manager is in charge of one license at most, so the list has one element or none. Each "
                    + "license says whether it is still in force in active.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The licenses of the manager",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = CooperativeLicenseResource.class)))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a technical manager")
    })
    public List<CooperativeLicenseResource> list() {
        var query = new GetAllCooperativeLicensesByManagerIdQuery(users.requireCurrentUserId());
        return cooperativeLicenseQueryService.handle(query).stream()
                .map(cooperativeLicenseResourceAssembler::toResource)
                .toList();
    }

    /**
     * Reads a license the authenticated technical manager is in charge of.
     *
     * @param id the license
     * @return 200 with the license, or 404 for both a missing license and one another manager is in charge of
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get a license of the signed-in technical manager",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The license",
                    content = @Content(schema = @Schema(implementation = CooperativeLicenseResource.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a technical manager"),
            @ApiResponse(responseCode = "404", description = "No such license among the manager's")
    })
    public ResponseEntity<?> get(@PathVariable UUID id) {
        var query = new GetCooperativeLicenseByIdAndManagerIdQuery(id.toString(), users.requireCurrentUserId());
        return cooperativeLicenseQueryService.handle(query)
                .<ResponseEntity<?>>map(license -> ResponseEntity.ok(cooperativeLicenseResourceAssembler.toResource(license)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("license", id.toString())));
    }
}

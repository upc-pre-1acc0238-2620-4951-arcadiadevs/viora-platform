package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.InvitationCodeCommandService;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.InvitationCodeQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.IssueInvitationCodesCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RevokeInvitationCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetInvitationCodesByManagerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeFilter;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.InvitationCodeBatchResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.InvitationCodeResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.IssueInvitationCodesResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.InvitationCodeResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * REST controller of the invitation code aggregate, as the technical manager who issues them sees it.
 * <p>The license always comes from the session: each manager only reaches the codes of the license they are in charge
 * of.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/invitation-codes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Invitation Codes", description = "Codes a technical manager issues for the producers of their license")
public class InvitationCodeController {

    private final InvitationCodeCommandService invitationCodeCommandService;
    private final InvitationCodeQueryService invitationCodeQueryService;
    private final CurrentUserProvider users;
    private final Clock clock;

    /**
     * Creates the controller.
     *
     * @param invitationCodeCommandService the issue and revocation use cases
     * @param invitationCodeQueryService   the code reads
     * @param users                        the authenticated user
     * @param clock                        the application clock
     * @throws IllegalArgumentException if any dependency is null
     */
    public InvitationCodeController(InvitationCodeCommandService invitationCodeCommandService,
                                    InvitationCodeQueryService invitationCodeQueryService,
                                    CurrentUserProvider users,
                                    Clock clock) {
        if (invitationCodeCommandService == null || invitationCodeQueryService == null || users == null
                || clock == null) {
            throw new IllegalArgumentException("subscription.invitation_code_controller.dependency.null");
        }
        this.invitationCodeCommandService = invitationCodeCommandService;
        this.invitationCodeQueryService = invitationCodeQueryService;
        this.users = users;
        this.clock = clock;
    }

    /**
     * Issues a batch of codes of the authenticated manager's license.
     *
     * @param resource how many codes, their hectares and their validity
     * @return 201 with the codes in full, or a problem detail explaining why they were not issued
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Issue a batch of invitation codes",
            description = "Each code holds a seat and its hectares of the license until it expires or is revoked, and "
                    + "is unique among every license. The codes expire after the chosen days or when the license "
                    + "ends, whichever comes first.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Codes issued",
                    content = @Content(schema = @Schema(implementation = InvitationCodeBatchResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid quantity, hectares or validity"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403",
                    description = "The user is not a technical manager, or has no license in force"),
            @ApiResponse(responseCode = "409", description = "Not enough free seats (reason "
                    + "subscription.license.insufficient_seats, seatsFree) or hectares (reason "
                    + "subscription.license.insufficient_area, freeAreaHa)"),
            @ApiResponse(responseCode = "503", description = "Codes cannot be issued now")
    })
    public ResponseEntity<?> issue(@Valid @RequestBody IssueInvitationCodesResource resource) {
        var command = new IssueInvitationCodesCommand(users.requireCurrentUserId(), resource.quantity(),
                resource.hectaresPerCode(), resource.validityDays());
        var result = invitationCodeCommandService.handle(command);
        if (result.isFailure()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(result.failure().orElseThrow());
        }
        var batch = InvitationCodeResourceAssembler.toResource(result.success().orElseThrow());
        return ResponseEntity.created(URI.create("/api/v1/invitation-codes?status=available")).body(batch);
    }

    /**
     * Lists one group of the codes of the authenticated manager's license.
     *
     * @param status the group: available, redeemed or closed (expired and revoked)
     * @return 200 with the codes, newest batch first
     */
    @GetMapping
    @Operation(
            summary = "List the invitation codes of the signed-in technical manager",
            description = "Available codes come in full so they can be shared again; redeemed, expired and revoked "
                    + "ones only masked. A redeemed code names the producer who used it.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The codes",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = InvitationCodeResource.class)))),
            @ApiResponse(responseCode = "400", description = "Unknown status"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a technical manager")
    })
    public ResponseEntity<?> list(
            @Parameter(description = "available, redeemed or closed")
            @RequestParam(defaultValue = "available") String status) {
        var filter = Arrays.stream(InvitationCodeFilter.values())
                .filter(each -> each.name().equalsIgnoreCase(status.trim()))
                .findFirst();
        if (filter.isEmpty()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.validationError("status", "subscription.invitation_code.filter.unknown"));
        }
        var query = new GetInvitationCodesByManagerIdQuery(users.requireCurrentUserId(), filter.get());
        List<InvitationCodeResource> codes = invitationCodeQueryService.handle(query).stream()
                .map(InvitationCodeResourceAssembler::toResource)
                .toList();
        return ResponseEntity.ok(codes);
    }

    /**
     * Revokes a code of the authenticated manager's license nobody redeemed yet.
     *
     * @param id the code
     * @return 200 with the revoked code, or a problem detail explaining why it was not revoked
     */
    @PostMapping("/{id}/revocations")
    @Operation(
            summary = "Revoke an invitation code",
            description = "Nobody can use it afterwards; its seat and hectares return to the license.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Code revoked",
                    content = @Content(schema = @Schema(implementation = InvitationCodeResource.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a technical manager"),
            @ApiResponse(responseCode = "404", description = "No such code among the manager's"),
            @ApiResponse(responseCode = "409", description = "Already redeemed (reason "
                    + "subscription.invitation_code.already_redeemed, redeemedAt) or no longer available (reason "
                    + "subscription.invitation_code.not_available)")
    })
    public ResponseEntity<?> revoke(@PathVariable UUID id) {
        var result = invitationCodeCommandService.handle(
                new RevokeInvitationCodeCommand(users.requireCurrentUserId(), id.toString()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                code -> InvitationCodeResourceAssembler.toResource(code, clock.instant()), HttpStatus.OK);
    }
}

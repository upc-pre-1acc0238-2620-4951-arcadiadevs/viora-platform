package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.ManagerCodeRedemptionCommandService;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemManagerCodeCommand;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.CooperativeLicenseResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.RedeemManagerCodeResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.CooperativeLicenseResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Clock;

/**
 * REST controller where a technical manager redeems the code that puts them in charge of their organization's
 * license.
 * <p>The manager is always the authenticated user; the request never names one.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/manager-code-redemptions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Manager Code Redemptions", description = "Activation of a cooperative license by its technical manager")
public class ManagerCodeRedemptionController {

    private final ManagerCodeRedemptionCommandService managerCodeRedemptionCommandService;
    private final CurrentUserProvider users;
    private final Clock clock;

    /**
     * Creates the controller.
     *
     * @param managerCodeRedemptionCommandService the redemption use case
     * @param users                               the authenticated user
     * @param clock                               the application clock
     * @throws IllegalArgumentException if any dependency is null
     */
    public ManagerCodeRedemptionController(ManagerCodeRedemptionCommandService managerCodeRedemptionCommandService,
                                           CurrentUserProvider users,
                                           Clock clock) {
        if (managerCodeRedemptionCommandService == null || users == null || clock == null) {
            throw new IllegalArgumentException("subscription.manager_redemption_controller.dependency.null");
        }
        this.managerCodeRedemptionCommandService = managerCodeRedemptionCommandService;
        this.users = users;
        this.clock = clock;
    }

    /**
     * Redeems a manager activation code for the authenticated technical manager.
     *
     * @param resource the code Viora gave the organization
     * @return 201 with the license and its location, or a problem detail explaining why the code was not redeemed
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Redeem a manager activation code",
            description = "Consumes the code and puts the authenticated technical manager in charge of its license, "
                    + "replacing the previous manager. A code that does not exist, expired, was used or revoked, or "
                    + "whose license is not in force answers 400 with reason subscription.manager_code.not_redeemable.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Code redeemed; the manager is in charge of the license",
                    content = @Content(schema = @Schema(implementation = CooperativeLicenseResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload, or a code that cannot be redeemed"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a technical manager"),
            @ApiResponse(responseCode = "409", description = "The manager is already in charge of a license in force"),
            @ApiResponse(responseCode = "429", description = "Too many failed attempts; see Retry-After")
    })
    public ResponseEntity<?> redeem(@Valid @RequestBody RedeemManagerCodeResource resource) {
        var managerId = users.requireCurrentUserId();
        var command = new RedeemManagerCodeCommand(managerId, resource.code());
        var result = managerCodeRedemptionCommandService.handle(command);
        if (result.isFailure()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(result.failure().orElseThrow());
        }
        var license = CooperativeLicenseResourceAssembler.toResource(result.success().orElseThrow(), clock.instant());
        return ResponseEntity.created(URI.create("/api/v1/cooperative-licenses/" + license.id())).body(license);
    }
}

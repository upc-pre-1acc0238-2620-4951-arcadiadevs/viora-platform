package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CooperativeCodeRedemptionCommandService;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.RedeemCooperativeCodeResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.SubscriptionResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.RedeemCooperativeCodeCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.SubscriptionResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * REST controller where a producer redeems the invitation code their cooperative gave them.
 * <p>The producer is always the authenticated user; the request never names one. A successful redemption answers
 * the sponsored subscription it activated.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/cooperative-code-redemptions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Cooperative Code Redemptions", description = "Redemption of cooperative invitation codes by producers")
public class CooperativeCodeRedemptionController {

    private final CooperativeCodeRedemptionCommandService cooperativeCodeRedemptionCommandService;
    private final SubscriptionResourceFromEntityAssembler subscriptionResourceAssembler;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the controller.
     *
     * @param cooperativeCodeRedemptionCommandService the redemption use case
     * @param subscriptionResourceAssembler           the assembler of the sponsored subscription
     * @param currentUserProvider                     the authenticated user
     * @throws IllegalArgumentException if any dependency is null
     */
    public CooperativeCodeRedemptionController(
            CooperativeCodeRedemptionCommandService cooperativeCodeRedemptionCommandService,
            SubscriptionResourceFromEntityAssembler subscriptionResourceAssembler,
            CurrentUserProvider currentUserProvider
    ) {
        if (cooperativeCodeRedemptionCommandService == null) {
            throw new IllegalArgumentException("subscription.redemption_service.null");
        }
        if (subscriptionResourceAssembler == null) {
            throw new IllegalArgumentException("subscription.resource_assembler.null");
        }
        if (currentUserProvider == null) {
            throw new IllegalArgumentException("subscription.current_user_provider.null");
        }
        this.cooperativeCodeRedemptionCommandService = cooperativeCodeRedemptionCommandService;
        this.subscriptionResourceAssembler = subscriptionResourceAssembler;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Redeems a cooperative invitation code for the authenticated producer.
     *
     * @param resource the code the cooperative gave the producer
     * @return 201 with the sponsored subscription, or a problem detail explaining why the code was not redeemed
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Redeem a cooperative invitation code",
            description = "Consumes the code and activates a subscription sponsored by the cooperative until the "
                    + "license ends, covering the hectares the code was issued for.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Code redeemed and sponsored subscription activated",
                    content = @Content(schema = @Schema(implementation = SubscriptionResource.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid payload, or a code that does not exist, expired, was redeemed or revoked"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a producer"),
            @ApiResponse(responseCode = "409", description = "The producer already has an active subscription"),
            @ApiResponse(responseCode = "429", description = "Too many failed attempts; see Retry-After")
    })
    public ResponseEntity<?> redeem(@Valid @RequestBody RedeemCooperativeCodeResource resource) {
        var producerId = currentUserProvider.requireCurrentUserId();
        var command = RedeemCooperativeCodeCommandFromResourceAssembler.toCommandFromResource(producerId, resource);
        var result = cooperativeCodeRedemptionCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(result, subscriptionResourceAssembler::toResource,
                HttpStatus.CREATED);
    }
}

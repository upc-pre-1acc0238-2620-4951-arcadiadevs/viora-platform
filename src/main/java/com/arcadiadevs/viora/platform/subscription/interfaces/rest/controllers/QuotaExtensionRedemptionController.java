package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.QuotaExtensionCommandService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemExtensionCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.QuotaExtensionResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.RedeemExtensionCodeResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.QuotaExtensionResourceAssembler;
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
 * REST controller where a producer adds the hectares of an extension code to the plan their license sponsors.
 * <p>The producer is always the authenticated user; the request never names one.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/quota-extension-redemptions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Plan Extensions", description = "Hectares added to a plan in force without changing when it ends")
public class QuotaExtensionRedemptionController {

    private final QuotaExtensionCommandService commands;
    private final SubscriptionRepository subscriptions;
    private final CurrentUserProvider users;

    /**
     * Creates the controller.
     *
     * @param commands      the extension use cases
     * @param subscriptions the plans, to show the quota after the extension
     * @param users         the authenticated actor
     */
    public QuotaExtensionRedemptionController(QuotaExtensionCommandService commands,
                                              SubscriptionRepository subscriptions, CurrentUserProvider users) {
        this.commands = commands;
        this.subscriptions = subscriptions;
        this.users = users;
    }

    /**
     * Redeems an extension code for the authenticated producer.
     *
     * @param resource the code the license shared
     * @return 201 with the applied extension, or a problem detail explaining why the code was not redeemed
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Redeem an extension code",
            description = "Adds the code's hectares to the producer's sponsored plan in force, once per producer, "
                    + "while the license has hectares left. The plan keeps its end date.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Hectares added",
                    content = @Content(schema = @Schema(implementation = QuotaExtensionResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload, or a code that cannot be used"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a producer"),
            @ApiResponse(responseCode = "429", description = "Too many failed attempts; see Retry-After")
    })
    public ResponseEntity<?> redeem(@Valid @RequestBody RedeemExtensionCodeResource resource) {
        var producerId = users.requireAuthenticatedUserId();
        var result = commands.handle(new RedeemExtensionCodeCommand(producerId, resource.code()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result, this::toResource, HttpStatus.CREATED);
    }

    private QuotaExtensionResource toResource(QuotaExtension extension) {
        var plan = subscriptions.findById(extension.snapshot().subscriptionId()).orElseThrow();
        return QuotaExtensionResourceAssembler.toResource(extension, plan, null);
    }
}

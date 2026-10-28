package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import java.net.URI;
import java.time.Clock;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.CreateSubscriptionCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.CreateSubscriptionResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.SubscriptionDetailsAssembler;

/** Authenticated HTTP boundary for individual contract creation only. */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class CreateSubscriptionController {
    private final SubscriptionCommandService commands;
    private final SubscriptionRepository subscriptions;
    private final CurrentUserProvider users;
    private final Clock clock;

    /**
     * Creates the individual contract HTTP boundary.
     * @param commands individual purchase use case
     * @param subscriptions domain read port
     * @param users verified actor context
     * @param clock application time
     */
    public CreateSubscriptionController(SubscriptionCommandService commands, SubscriptionRepository subscriptions,
            CurrentUserProvider users, Clock clock) {
        this.commands = commands;
        this.subscriptions = subscriptions;
        this.users = users;
        this.clock = clock;
    }

    /**
     * Creates or recovers a pending purchase using a real signed-in identity.
     * @param request selected plan and capacity
     * @param key durable operation key
     * @return created or replayed pending contract
     */
    @PostMapping
    @Operation(summary = "Create an individual subscription pending payment")
    public ResponseEntity<?> create(@Valid @RequestBody CreateSubscriptionResource request,
            @RequestHeader("Idempotency-Key") String key) {
        var owner = users.requireCurrentUserId();
        var result = commands.handle(new CreateSubscriptionCommand(owner, request.planCode(), request.requestedQuotaHa(), key));
        if (result.isFailure()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(result.failure().orElseThrow());
        }
        var created = result.success().orElseThrow();
        var subscription = subscriptions.findById(new SubscriptionId(created.id().toString())).orElseThrow();
        return ResponseEntity.status(created.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .location(URI.create("/api/v1/subscriptions/" + created.id()))
                .body(SubscriptionDetailsAssembler.toResource(subscription, clock.instant()));
    }
}

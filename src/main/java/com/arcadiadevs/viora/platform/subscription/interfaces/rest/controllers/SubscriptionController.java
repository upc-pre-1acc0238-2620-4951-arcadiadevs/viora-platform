package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import java.net.URI;
import java.time.Clock;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.arcadiadevs.viora.platform.shared.application.result.*;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.internal.queryservices.SubscriptionQueryService;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionCatalog;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.CreateSubscriptionCommand;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.*;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.SubscriptionResourceAssembler;
/** REST boundary for initial annual contracts. Identity comes exclusively from the verified context. */
@RestController @RequestMapping("/api/v1") @Tag(name = "Subscriptions")
public class SubscriptionController {
    private final SubscriptionCatalog catalog;
    private final SubscriptionCommandService commands;
    private final SubscriptionQueryService queries;
    private final CurrentUserProvider users;
    private final Clock clock;
    /** @param catalog tariff @param commands writes @param queries reads @param users actor @param clock time */
    public SubscriptionController(SubscriptionCatalog catalog, SubscriptionCommandService commands,
            SubscriptionQueryService queries, CurrentUserProvider users, Clock clock) {
        this.catalog = catalog; this.commands = commands; this.queries = queries; this.users = users; this.clock = clock;
    }
    /** @return server-owned catalogue */
    @GetMapping("/subscription-plans") @Operation(summary = "List initial producer plan offers")
    public List<SubscriptionCatalog.PlanOffer> plans() { return catalog.plans(); }
    /** @param resource selected offer @param key durable request identifier @return created contract or replay */
    @PostMapping("/subscriptions") @Operation(summary = "Create a pending subscription; does not initiate payment")
    public ResponseEntity<?> create(@Valid @RequestBody CreateSubscriptionResource resource,
            @RequestHeader("Idempotency-Key") String key) {
        var owner = users.requireCurrentUserId();
        var result = commands.handle(new CreateSubscriptionCommand(owner, resource.planCode(), resource.requestedQuotaHa(), key));
        if (result instanceof Result.Failure<SubscriptionCommandService.CreatedSubscription, ApplicationError> failure) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        }
        var created = result.success().orElseThrow();
        var snapshot = queries.find(owner, created.id()).orElseThrow();
        return ResponseEntity.status(created.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .location(URI.create("/api/v1/subscriptions/" + created.id()))
                .body(SubscriptionResourceAssembler.toResource(snapshot, clock.instant()));
    }
    /** @return latest owned contract, including pending/expired; 204 if no contract exists */
    @GetMapping("/subscriptions") @Operation(summary = "Get the current producer contract, including pending payment")
    public ResponseEntity<?> current() {
        var current = queries.current(users.requireCurrentUserId());
        if (current.isEmpty()) { return ResponseEntity.noContent().build(); }
        return ResponseEntity.ok(SubscriptionResourceAssembler.toResource(current.get(), clock.instant()));
    }
    /** @param id contract identifier @return owned contract; other owners receive 404 */
    @GetMapping("/subscriptions/{id}") @Operation(summary = "Get an owned subscription")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        var found = queries.find(users.requireCurrentUserId(), id);
        if (found.isEmpty()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("subscription", id.toString()));
        }
        return ResponseEntity.ok(SubscriptionResourceAssembler.toResource(found.get(), clock.instant()));
    }
}

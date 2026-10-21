package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import java.time.Clock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionQueryService;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.SubscriptionDetailsAssembler;

/** Read boundary for the signed-in producer's current contract. */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class CurrentSubscriptionController {
    private final SubscriptionQueryService queries;
    private final CurrentUserProvider users;
    private final Clock clock;

    /**
     * Creates the current-contract boundary.
     * @param queries owned contract queries
     * @param users verified actor context
     * @param clock application clock
     */
    public CurrentSubscriptionController(SubscriptionQueryService queries, CurrentUserProvider users, Clock clock) {
        this.queries = queries;
        this.users = users;
        this.clock = clock;
    }

    /**
     * Reads only the signed-in producer's contract without granting or changing access.
     * @return current owned contract, or 204 when none exists
     * @throws com.arcadiadevs.viora.platform.shared.application.security.AuthenticationRequiredException
     *         if no real authenticated identity is available, including in demo mode
     */
    @GetMapping
    @Operation(summary = "Get the signed-in producer's current subscription")
    public ResponseEntity<?> current() {
        var current = queries.current(users.requireAuthenticatedUserId());
        return current.<ResponseEntity<?>>map(subscription -> ResponseEntity.ok(
                SubscriptionDetailsAssembler.toResource(subscription, clock.instant())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}

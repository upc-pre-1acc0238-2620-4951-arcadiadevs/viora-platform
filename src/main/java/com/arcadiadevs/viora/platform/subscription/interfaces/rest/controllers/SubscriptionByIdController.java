package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import java.time.Clock;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionQueryService;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.SubscriptionDetailsAssembler;

/** Read boundary for a specific contract owned by the signed-in producer. */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionByIdController {
    private final SubscriptionQueryService queries;
    private final CurrentUserProvider users;
    private final Clock clock;

    /**
     * Creates the owned lookup boundary.
     * @param queries owned domain queries
     * @param users authenticated actor
     * @param clock application clock
     */
    public SubscriptionByIdController(SubscriptionQueryService queries, CurrentUserProvider users, Clock clock) {
        this.queries = queries;
        this.users = users;
        this.clock = clock;
    }

    /**
     * Hides both nonexistent and foreign contracts with the same not-found response.
     * @param id requested contract
     * @return owned details or a not-found problem
     * @throws com.arcadiadevs.viora.platform.shared.application.security.AuthenticationRequiredException
     *         if no authenticated identity is available, including in demo mode
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a subscription owned by the signed-in producer")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        var found = queries.find(users.requireAuthenticatedUserId(), id);
        if (found.isEmpty()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.notFound("subscription", id.toString()));
        }
        return ResponseEntity.ok(SubscriptionDetailsAssembler.toResource(found.orElseThrow(), clock.instant()));
    }
}

package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.security.AuthenticationRequiredException;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionCatalog;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.CreateSubscriptionCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.CreateSubscriptionResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.SubscriptionDetailsAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * REST controller of the subscription aggregate: its plan offer, the purchase of an individual contract, its reads and
 * its checkout. Only the authenticated producer's contracts are reachable, except for the public plan offer.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Subscriptions", description = "Plans, contracts and checkouts of individual producer subscriptions")
public class SubscriptionController {

    private final SubscriptionCatalog catalog;
    private final SubscriptionCommandService commands;
    private final SubscriptionQueryService queries;
    private final CheckoutService payments;
    private final SubscriptionRepository subscriptions;
    private final CurrentUserProvider users;
    private final Clock clock;

    /**
     * Creates the controller.
     *
     * @param catalog       server-owned plan query contract
     * @param commands      individual purchase use case
     * @param queries       owned contract queries
     * @param payments      durable payment use case
     * @param subscriptions domain read port
     * @param users         verified actor context
     * @param clock         application clock
     */
    public SubscriptionController(SubscriptionCatalog catalog, SubscriptionCommandService commands,
                                  SubscriptionQueryService queries, CheckoutService payments,
                                  SubscriptionRepository subscriptions, CurrentUserProvider users, Clock clock) {
        this.catalog = catalog;
        this.commands = commands;
        this.queries = queries;
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.users = users;
        this.clock = clock;
    }

    /**
     * Lists the commercial conditions without creating a contract or payment.
     *
     * @return available individual producer offers
     */
    @GetMapping("/subscription-plans")
    @Operation(summary = "List individual producer subscription plans")
    public List<SubscriptionCatalog.PlanOffer> plans() {
        return catalog.plans();
    }

    /**
     * Creates or recovers a pending purchase using a real signed-in identity.
     *
     * @param request selected plan and capacity
     * @param key     durable operation key
     * @return created or replayed pending contract
     */
    @PostMapping("/subscriptions")
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

    /**
     * Reads only the signed-in producer's contract without granting or changing access.
     *
     * @return current owned contract, or 204 when none exists
     * @throws AuthenticationRequiredException if no real authenticated identity is available, including in demo mode
     */
    @GetMapping("/subscriptions")
    @Operation(summary = "Get the signed-in producer's current subscription")
    public ResponseEntity<?> current() {
        var current = queries.current(users.requireCurrentUserId());
        return current.<ResponseEntity<?>>map(subscription -> ResponseEntity.ok(
                SubscriptionDetailsAssembler.toResource(subscription, clock.instant(),
                        queries.paymentStatus(users.requireCurrentUserId(),
                                UUID.fromString(subscription.snapshot().id().subscriptionId())))))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Hides both nonexistent and foreign contracts with the same not-found response.
     *
     * @param id requested contract
     * @return owned details or a not-found problem
     * @throws AuthenticationRequiredException if no authenticated identity is available, including in demo mode
     */
    @GetMapping("/subscriptions/{id}")
    @Operation(summary = "Get a subscription owned by the signed-in producer")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        var found = queries.find(users.requireCurrentUserId(), id);
        if (found.isEmpty()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.notFound("subscription", id.toString()));
        }
        return ResponseEntity.ok(SubscriptionDetailsAssembler.toResource(found.orElseThrow(), clock.instant(),
                queries.paymentStatus(users.requireCurrentUserId(), id)));
    }

    /**
     * Returns a hosted link without granting access.
     *
     * @param id  owned contract
     * @param key durable operation key
     * @return reusable checkout or consistent problem
     */
    @PostMapping("/subscriptions/{id}/checkouts")
    @Operation(summary = "Prepare or recover an owned Mercado Pago checkout")
    public ResponseEntity<?> checkout(@PathVariable UUID id, @RequestHeader("Idempotency-Key") String key) {
        return ResponseEntityAssembler.toResponseEntityFromResult(
                payments.create(users.requireCurrentUserId(), id, key), result -> result, HttpStatus.CREATED);
    }
}

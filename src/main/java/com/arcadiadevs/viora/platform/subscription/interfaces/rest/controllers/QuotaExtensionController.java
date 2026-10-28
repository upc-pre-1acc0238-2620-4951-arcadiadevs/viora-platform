package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.QuotaExtensionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.QuotaExtensionQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RequestQuotaExtensionCommand;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.QuotaExtensionResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.RequestQuotaExtensionResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.QuotaExtensionResourceAssembler;
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
import java.util.UUID;

/**
 * Authenticated boundary for growing an owned plan in force: its terms, paid extensions and their checkouts. The
 * hectares of a paid extension are added only when Mercado Pago approves the payment.
 */
@RestController
@RequestMapping("/api/v1/subscriptions/{subscriptionId}")
@Tag(name = "Plan Extensions", description = "Hectares added to a plan in force without changing when it ends")
public class QuotaExtensionController {

    private final QuotaExtensionCommandService commands;
    private final QuotaExtensionQueryService queries;
    private final CheckoutService payments;
    private final SubscriptionRepository subscriptions;
    private final CurrentUserProvider users;

    /**
     * Creates the controller.
     *
     * @param commands      the extension use cases
     * @param queries       the extension reads
     * @param payments      the durable checkout use case
     * @param subscriptions the plans, to show the quota after an extension
     * @param users         the authenticated actor
     */
    public QuotaExtensionController(QuotaExtensionCommandService commands, QuotaExtensionQueryService queries,
                                    CheckoutService payments, SubscriptionRepository subscriptions,
                                    CurrentUserProvider users) {
        this.commands = commands;
        this.queries = queries;
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.users = users;
    }

    /**
     * Tells how the plan can grow now.
     *
     * @param subscriptionId the owned plan
     * @return the terms, or a not-found problem for a missing or foreign plan
     */
    @GetMapping("/extension-terms")
    @Operation(summary = "Get how an owned plan can grow now")
    public ResponseEntity<?> terms(@PathVariable UUID subscriptionId) {
        return queries.terms(users.requireCurrentUserId(), subscriptionId)
                .<ResponseEntity<?>>map(terms -> ResponseEntity.ok(QuotaExtensionResourceAssembler.toResource(terms)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("subscription", subscriptionId.toString())));
    }

    /**
     * Requests paid hectares for an individual plan, or replays the same request.
     *
     * @param subscriptionId the owned plan
     * @param request        the hectares to add
     * @param key            the durable operation key
     * @return 201 with the pending extension (200 when replayed), or a problem detail
     */
    @PostMapping("/extensions")
    @Operation(summary = "Request a paid extension of an individual plan, pending payment")
    public ResponseEntity<?> request(@PathVariable UUID subscriptionId,
                                     @Valid @RequestBody RequestQuotaExtensionResource request,
                                     @RequestHeader("Idempotency-Key") String key) {
        var owner = users.requireCurrentUserId();
        var result = commands.handle(new RequestQuotaExtensionCommand(owner, subscriptionId, request.addedHa(), key));
        if (result.isFailure()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(result.failure().orElseThrow());
        }
        var requested = result.success().orElseThrow();
        var extension = requested.extension();
        return ResponseEntity.status(requested.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .location(URI.create("/api/v1/subscriptions/" + subscriptionId + "/extensions/"
                        + extension.snapshot().id().quotaExtensionId()))
                .body(toResource(extension, null));
    }

    /**
     * Reads an extension of an owned plan, such as while the app waits for its payment.
     *
     * @param subscriptionId the owned plan
     * @param extensionId    the extension
     * @return the extension, or a not-found problem for a missing or foreign one
     */
    @GetMapping("/extensions/{extensionId}")
    @Operation(summary = "Get an extension of an owned plan")
    public ResponseEntity<?> get(@PathVariable UUID subscriptionId, @PathVariable UUID extensionId) {
        return queries.find(users.requireCurrentUserId(), subscriptionId, extensionId)
                .<ResponseEntity<?>>map(found -> ResponseEntity.ok(toResource(found, queries.paymentStatus(extensionId))))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("quota_extension", extensionId.toString())));
    }

    /**
     * Returns a hosted Mercado Pago link for a pending paid extension, without adding hectares.
     *
     * @param subscriptionId the owned plan
     * @param extensionId    the pending extension
     * @param key            the durable operation key
     * @return a reusable checkout or a problem detail
     */
    @PostMapping("/extensions/{extensionId}/checkouts")
    @Operation(summary = "Prepare or recover the Mercado Pago checkout of a pending extension")
    public ResponseEntity<?> checkout(@PathVariable UUID subscriptionId, @PathVariable UUID extensionId,
                                      @RequestHeader("Idempotency-Key") String key) {
        return ResponseEntityAssembler.toResponseEntityFromResult(
                payments.createForExtension(users.requireCurrentUserId(), subscriptionId, extensionId, key),
                result -> result, HttpStatus.CREATED);
    }

    private QuotaExtensionResource toResource(QuotaExtension extension, String paymentStatus) {
        var plan = subscriptions.findById(extension.snapshot().subscriptionId()).orElseThrow();
        return QuotaExtensionResourceAssembler.toResource(extension, plan, paymentStatus);
    }
}

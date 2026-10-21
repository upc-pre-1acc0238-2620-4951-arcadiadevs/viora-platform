package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import com.arcadiadevs.viora.platform.shared.application.result.*;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.*;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import com.arcadiadevs.viora.platform.subscription.infrastructure.mercadopago.MercadoPagoWebhookAuthenticator;
import io.swagger.v3.oas.annotations.Operation;
/** Checkout and authenticated provider notification entry points. */
@RestController @RequestMapping("/api/v1")
public class CheckoutController {
    private final CheckoutService payments;
    private final com.arcadiadevs.viora.platform.subscription.application.internal.queryservices.SubscriptionQueryService queries;
    private final CurrentUserProvider users;
    private final MercadoPagoWebhookAuthenticator signatures;
    public CheckoutController(CheckoutService payments, CurrentUserProvider users, MercadoPagoWebhookAuthenticator signatures, com.arcadiadevs.viora.platform.subscription.application.internal.queryservices.SubscriptionQueryService queries) {
        this.queries = queries; this.payments = payments; this.users = users; this.signatures = signatures;
    }
    @PostMapping("/subscriptions/{id}/checkouts") @Operation(summary = "Prepare or recover an owned checkout")
    public ResponseEntity<?> create(@PathVariable UUID id, @RequestHeader("Idempotency-Key") String key) {
        return ResponseEntityAssembler.toResponseEntityFromResult(payments.create(users.requireCurrentUserId(), id, key), r -> r, HttpStatus.CREATED);
    }
    @GetMapping("/subscriptions/{id}/payment-status") @Operation(summary = "Read the last stored payment state without querying the provider")
    public ResponseEntity<?> status(@PathVariable UUID id) {
        if (queries.find(users.requireCurrentUserId(), id).isEmpty()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("subscription", id.toString()));
        }
        return ResponseEntity.ok(Map.of("status", payments.latest(id).map(a -> a.status()).orElse("NOT_STARTED")));
    }
    public record Notification(String type, Data data) { public record Data(String id) { } }
    @PostMapping("/payment-notifications/mercado-pago") @Operation(summary = "Verify and reconcile a Mercado Pago payment notification")
    public ResponseEntity<?> notification(@RequestParam(name = "data.id", required = false) String id,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestBody Notification body) {
        if (!signatures.configured()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(new ApplicationError("PAYMENT_UNAVAILABLE", "subscription.gateway.unavailable"));
        }
        if (!signatures.valid(id, requestId, signature) || body == null || body.data() == null
                || !Objects.equals(id, body.data().id()) || !"payment".equals(body.type())) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(new ApplicationError("PAYMENT_UNAUTHORIZED", "subscription.signature.invalid"));
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(payments.confirm(id), r -> Map.of("received", true), HttpStatus.OK);
    }
}

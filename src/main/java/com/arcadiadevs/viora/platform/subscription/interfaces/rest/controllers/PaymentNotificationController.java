package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import java.util.Map;
import java.util.Objects;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.*;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import com.arcadiadevs.viora.platform.subscription.application.ports.PaymentNotificationAuthenticator;

/** Authenticated provider boundary; browser callbacks cannot invoke payment activation. */
@RestController
@RequestMapping("/api/v1/payment-notifications/mercado-pago")
public class PaymentNotificationController {
    private final CheckoutService payments;
    private final PaymentNotificationAuthenticator signatures;

    /**
     * Creates the signed notification boundary.
     * @param payments authoritative reconciliation use case
     * @param signatures provider envelope authentication
     */
    public PaymentNotificationController(CheckoutService payments, PaymentNotificationAuthenticator signatures) {
        this.payments = payments;
        this.signatures = signatures;
    }

    /**
     * Provider notification payload, with no trusted monetary or entitlement fields.
     * @param type event category
     * @param data external transaction reference
     */
    public record Notification(String type, Data data) {
        /** @param id external payment identity */
        public record Data(String id) { }
    }

    /**
     * Authenticates the notification and acknowledges only durable reconciliation.
     * @param id canonical query identifier
     * @param requestId provider request header
     * @param signature provider signature header
     * @param body notification envelope
     * @return acknowledgement or a retryable/authentication problem
     */
    @PostMapping
    @Operation(summary = "Authenticate and reconcile a Mercado Pago payment notification")
    public ResponseEntity<?> notification(@RequestParam(name = "data.id", required = false) String id,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestBody Notification body) {
        if (!signatures.configured()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    new ApplicationError("PAYMENT_UNAVAILABLE", "subscription.gateway.unavailable"));
        }
        if (!signatures.valid(id, requestId, signature) || body == null || body.data() == null
                || !Objects.equals(id, body.data().id()) || !"payment".equals(body.type())) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    new ApplicationError("PAYMENT_UNAUTHORIZED", "subscription.signature.invalid"));
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(
                payments.confirm(id), result -> Map.of("received", true), HttpStatus.OK);
    }
}

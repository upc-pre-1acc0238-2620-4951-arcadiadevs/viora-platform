package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;

/** Authenticated boundary for creating or recovering an owned individual checkout. */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class CheckoutController {
    private final CheckoutService payments;
    private final CurrentUserProvider users;

    /**
     * Creates the checkout boundary.
     * @param payments durable payment use case
     * @param users verified actor context
     */
    public CheckoutController(CheckoutService payments, CurrentUserProvider users) {
        this.payments = payments;
        this.users = users;
    }

    /**
     * Returns a hosted link without granting access.
     * @param id owned contract
     * @param key durable operation key
     * @return reusable checkout or consistent problem
     */
    @PostMapping("/{id}/checkouts")
    @Operation(summary = "Prepare or recover an owned Mercado Pago checkout")
    public ResponseEntity<?> create(@PathVariable UUID id, @RequestHeader("Idempotency-Key") String key) {
        return ResponseEntityAssembler.toResponseEntityFromResult(
                payments.create(users.requireCurrentUserId(), id, key), result -> result, HttpStatus.CREATED);
    }
}

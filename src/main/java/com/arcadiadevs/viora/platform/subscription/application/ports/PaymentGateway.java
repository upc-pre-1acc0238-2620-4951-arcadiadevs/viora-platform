package com.arcadiadevs.viora.platform.subscription.application.ports;

import java.time.Instant;
import java.util.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
/** Anti-corruption boundary; provider objects and credentials stay in infrastructure. */
public interface PaymentGateway {
    boolean configured();
    Checkout create(PaymentAttempt attempt, SubscriptionPrice price);
    Optional<Checkout> recover(UUID intentId);
    VerifiedPayment verify(String paymentId);
    List<VerifiedPayment> findPayments(UUID intentId);
    record Checkout(String preferenceId, String checkoutUrl) { }
    record VerifiedPayment(String id, UUID intentId, String status, Money amount, String currency,
            Instant initiatedAt, Instant approvedAt) { }
    /** Sanitized transient boundary failure; never contains provider bodies or credentials. */
    class Unavailable extends RuntimeException {
        public Unavailable() { super("subscription.gateway.unavailable"); }
    }
}

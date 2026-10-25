package com.arcadiadevs.viora.platform.subscription.application.ports;

import java.time.Instant;
import java.util.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;

/** Anti-corruption boundary; provider objects and credentials stay in infrastructure. */
public interface PaymentGateway {
    /** @return whether hosted payments are explicitly enabled and fully configured */
    boolean configured();

    /**
     * Creates a provider preference for an already persisted attempt.
     * @param attempt durable identity and configured validity window
     * @param price immutable server-calculated contract terms
     * @return provider-hosted navigation data
     * @throws Unavailable if the provider result cannot be established safely
     */
    Checkout create(PaymentAttempt attempt, SubscriptionPrice price);

    /**
     * Searches for a previously created preference without issuing another create request.
     * @param intentId durable attempt reference
     * @return recovered navigation data when a unique matching preference exists
     * @throws Unavailable if the remote result cannot be established safely
     */
    Optional<Checkout> recover(UUID intentId);

    /**
     * Retrieves a transaction and verifies provider environment and receiving account.
     * @param paymentId provider transaction identifier
     * @return authoritative payment evidence
     * @throws Unavailable if evidence cannot be obtained or validated
     */
    VerifiedPayment verify(String paymentId);

    /**
     * Retrieves verified transactions belonging to one durable attempt.
     * @param intentId durable attempt reference
     * @return verified matching transaction observations
     * @throws Unavailable if the search cannot establish a complete trustworthy result
     */
    List<VerifiedPayment> findPayments(UUID intentId);

    /**
     * Ends the validity of a hosted preference, so its link can no longer start a payment.
     * @param preferenceId provider preference identifier
     * @param at instant from which the link stops accepting payments
     * @throws Unavailable if the provider does not confirm the change
     */
    void close(String preferenceId, Instant at);

    /**
     * Provider-hosted navigation information without credentials.
     * @param preferenceId provider preference identifier
     * @param checkoutUrl validated hosted payment URL
     */
    record Checkout(String preferenceId, String checkoutUrl) { }

    /**
     * Authoritative transaction evidence normalized at the infrastructure boundary.
     * @param id provider transaction identifier
     * @param intentId correlated durable attempt reference
     * @param status provider transaction state
     * @param amount verified transaction total
     * @param currency verified settlement currency
     * @param initiatedAt provider transaction initiation time
     * @param approvedAt approval time, absent when not approved
     */
    record VerifiedPayment(String id, UUID intentId, String status, Money amount, String currency,
            Instant initiatedAt, Instant approvedAt) { }

    /** Sanitized transient boundary failure; never contains provider bodies or credentials. */
    class Unavailable extends RuntimeException {
        /** Creates a sanitized provider-unavailable failure using the shared message key. */
        public Unavailable() { super("subscription.gateway.unavailable"); }
    }
}

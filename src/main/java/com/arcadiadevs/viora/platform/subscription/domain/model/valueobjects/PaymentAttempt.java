package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.time.Instant;
import java.util.*;

/** Immutable attempt with transaction-specific observations, separate from entitlement. */
public record PaymentAttempt(UUID id, UUID subscriptionId, String producerId, String status,
        String preferenceId, String checkoutUrl, Instant createdAt, Instant expiresAt, Map<String, String> paymentStates) {
    /**
     * Validates a durable attempt and freezes its transaction observations.
     * @throws IllegalArgumentException if identity, lifetime, state or observations are invalid
     */
    public PaymentAttempt {
        if (id == null || subscriptionId == null || producerId == null || producerId.isBlank()
                || createdAt == null || expiresAt == null || !createdAt.isBefore(expiresAt)
                || status == null || paymentStates == null) {
            throw new IllegalArgumentException("subscription.payment.attempt.invalid");
        }
        try {
            PaymentAttemptState.valueOf(status);
            paymentStates = Map.copyOf(paymentStates);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("subscription.payment.attempt.invalid", exception);
        }
    }
    public PaymentAttempt(UUID id, UUID subscriptionId, String producerId, String status,
            String preferenceId, String checkoutUrl, Instant createdAt, Instant expiresAt) {
        this(id, subscriptionId, producerId, status, preferenceId, checkoutUrl, createdAt, expiresAt, Map.of());
    }
    public PaymentAttempt withCheckout(String preference, String url) {
        return new PaymentAttempt(id, subscriptionId, producerId, "CREATING".equals(status) ? "OPEN" : status,
                preference, url, createdAt, expiresAt, paymentStates);
    }
    /**
     * Updates attempt lifecycle without expiring an unresolved or approved payment.
     * @param next requested lifecycle state
     * @return immutable updated attempt
     * @throws IllegalStateException if expiration would discard pending or approved evidence
     */
    public PaymentAttempt withStatus(String next) {
        if ("EXPIRED".equals(next) && Set.of("PENDING", "APPROVED").contains(status)) {
            throw new IllegalStateException("subscription.payment.unresolved");
        }
        return new PaymentAttempt(id, subscriptionId, producerId, next, preferenceId, checkoutUrl, createdAt, expiresAt, paymentStates);
    }
    /**
     * Applies an authoritative transaction state; an unrelated rejection cannot hide a pending payment.
     * @param paymentId verified transaction identifier
     * @param next verified provider state
     * @return immutable aggregate projection
     */
    public PaymentAttempt withPaymentState(String paymentId, String next) {
        if (paymentId == null || !paymentId.matches("[0-9]{1,30}") || next == null
                || !Set.of("approved", "pending", "in_process", "authorized", "in_mediation", "rejected", "cancelled").contains(next)) {
            throw new IllegalArgumentException("subscription.payment.mismatch");
        }
        var observed = new HashMap<>(paymentStates);
        var previous = observed.get(paymentId);
        var pending = Set.of("pending", "in_process", "authorized", "in_mediation");
        if (!"approved".equals(previous) && !(Set.of("rejected", "cancelled").contains(previous == null ? "" : previous) && pending.contains(next))) {
            observed.put(paymentId, next);
        }
        String projected = status;
        if ("APPROVED".equals(status) || observed.containsValue("approved")) { projected = "APPROVED"; }
        else if (observed.values().stream().anyMatch(pending::contains)) { projected = "PENDING"; }
        else if (observed.values().stream().anyMatch(Set.of("rejected", "cancelled")::contains)) { projected = "REJECTED"; }
        return new PaymentAttempt(id, subscriptionId, producerId, projected, preferenceId, checkoutUrl, createdAt, expiresAt, observed);
    }
}

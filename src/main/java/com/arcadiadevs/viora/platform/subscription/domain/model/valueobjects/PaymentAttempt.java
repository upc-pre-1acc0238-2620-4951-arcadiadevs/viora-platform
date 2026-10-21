package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.time.Instant;
import java.util.UUID;
/** Immutable payment attempt, separate from entitlement. */
public record PaymentAttempt(UUID id, UUID subscriptionId, String producerId, String status,
        String preferenceId, String checkoutUrl, Instant createdAt, Instant expiresAt) {
    public PaymentAttempt withCheckout(String preference, String url) {
        return new PaymentAttempt(id, subscriptionId, producerId, "CREATING".equals(status) ? "OPEN" : status, preference, url, createdAt, expiresAt);
    }
    public PaymentAttempt withStatus(String next) {
        return new PaymentAttempt(id, subscriptionId, producerId, next, preferenceId, checkoutUrl, createdAt, expiresAt);
    }
}

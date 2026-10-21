package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/**
 * Lifecycle status of a subscription.
 */
public enum SubscriptionStatus {
    PENDING_PAYMENT,
    ACTIVE,
    EXPIRED,
    CANCELLED
}

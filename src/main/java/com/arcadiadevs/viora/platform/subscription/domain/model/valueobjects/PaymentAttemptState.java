package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/** Allowed durable attempt lifecycle states, separate from raw provider transaction states. */
public enum PaymentAttemptState {
    CREATING, OPEN, PENDING, APPROVED, REJECTED, EXPIRED
}

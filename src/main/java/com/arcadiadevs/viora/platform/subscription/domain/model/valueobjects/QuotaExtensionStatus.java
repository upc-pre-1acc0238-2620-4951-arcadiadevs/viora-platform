package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/**
 * Lifecycle status of a quota extension. A paid extension waits in {@code PENDING_PAYMENT} until its payment is
 * approved; an extension from a code is {@code APPLIED} at once. {@code APPLIED} is terminal.
 */
public enum QuotaExtensionStatus {
    PENDING_PAYMENT,
    APPLIED
}

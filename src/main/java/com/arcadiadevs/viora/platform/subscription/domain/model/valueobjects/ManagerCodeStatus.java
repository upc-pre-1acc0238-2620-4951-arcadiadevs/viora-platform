package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/**
 * Lifecycle status of a manager activation code. {@code REDEEMED}, {@code EXPIRED} and {@code REVOKED} are terminal:
 * only an {@code AVAILABLE} code can be redeemed or revoked.
 */
public enum ManagerCodeStatus {
    AVAILABLE,
    REDEEMED,
    EXPIRED,
    REVOKED
}

package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/**
 * Lifecycle status of a cooperative invitation code. {@code REDEEMED}, {@code EXPIRED} and {@code REVOKED} are
 * terminal: only an {@code AVAILABLE} code can be redeemed.
 */
public enum InvitationCodeStatus {
    AVAILABLE,
    REDEEMED,
    EXPIRED,
    REVOKED
}

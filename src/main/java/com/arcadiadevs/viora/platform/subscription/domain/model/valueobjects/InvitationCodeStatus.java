package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/**
 * Lifecycle status of a cooperative invitation code. {@code EXPIRED}, {@code REVOKED} and {@code RELEASED} are
 * terminal, and only an {@code AVAILABLE} code can be redeemed. A {@code REDEEMED} code is {@code RELEASED} when its
 * member leaves: its seat and hectares go back to the license.
 */
public enum InvitationCodeStatus {
    AVAILABLE,
    REDEEMED,
    EXPIRED,
    REVOKED,
    RELEASED
}

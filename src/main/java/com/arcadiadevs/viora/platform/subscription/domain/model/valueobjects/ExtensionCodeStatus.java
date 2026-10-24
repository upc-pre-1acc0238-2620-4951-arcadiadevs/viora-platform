package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/**
 * Lifecycle status of an extension code. An {@code ACTIVE} code can be redeemed by every producer of its license, once
 * each; {@code REVOKED} is terminal.
 */
public enum ExtensionCodeStatus {
    ACTIVE,
    REVOKED
}

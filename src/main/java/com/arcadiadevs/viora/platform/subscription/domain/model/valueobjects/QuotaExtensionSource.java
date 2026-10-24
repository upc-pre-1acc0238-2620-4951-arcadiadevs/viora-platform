package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/**
 * Where the hectares of a quota extension come from: a payment of the producer for an individual plan, or an
 * extension code of the license that sponsors the producer.
 */
public enum QuotaExtensionSource {
    PAYMENT,
    CODE
}

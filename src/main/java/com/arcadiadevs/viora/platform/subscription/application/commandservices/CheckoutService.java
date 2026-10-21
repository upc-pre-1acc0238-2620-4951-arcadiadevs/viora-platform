package com.arcadiadevs.viora.platform.subscription.application.commandservices;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import com.arcadiadevs.viora.platform.shared.application.result.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PaymentAttempt;

/** Application boundary for durable individual payment attempts and authoritative reconciliation. */
public interface CheckoutService {
    /**
     * Creates or recovers an owned checkout without granting access.
     * @param owner authenticated producer
     * @param subscriptionId owned contract
     * @param key durable operation key
     * @return checkout or consistent failure
     */
    Result<CheckoutResult, ApplicationError> create(String owner, UUID subscriptionId, String key);

    /**
     * Reconciles provider evidence, never browser return parameters.
     * @param paymentId authenticated provider transaction identifier
     * @return durable processing acknowledgement
     */
    Result<Boolean, ApplicationError> confirm(String paymentId);

    /**
     * Reads the latest stored attempt without contacting the provider.
     * @param subscriptionId owned contract identity
     * @return stored attempt when present
     */
    Optional<PaymentAttempt> latest(UUID subscriptionId);

    /**
     * Reusable hosted payment navigation data.
     * @param intentId durable attempt identity
     * @param checkoutUrl provider hosted URL
     * @param expiresAt exclusive link expiry
     */
    record CheckoutResult(UUID intentId, String checkoutUrl, Instant expiresAt) { }
}

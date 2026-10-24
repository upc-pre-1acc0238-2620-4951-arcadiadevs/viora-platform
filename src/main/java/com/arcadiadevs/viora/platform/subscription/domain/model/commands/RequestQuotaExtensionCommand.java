package com.arcadiadevs.viora.platform.subscription.domain.model.commands;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command asking to add paid hectares to the authenticated producer's individual plan.
 *
 * @param producerId     the authenticated producer
 * @param subscriptionId the plan that grows
 * @param addedHa        the hectares to add, in half-hectare steps
 * @param operationId    the idempotency key
 */
public record RequestQuotaExtensionCommand(String producerId, UUID subscriptionId, BigDecimal addedHa,
                                           String operationId) {

    /**
     * Compact constructor enforcing non-null fields.
     */
    public RequestQuotaExtensionCommand {
        if (producerId == null || subscriptionId == null || addedHa == null || operationId == null) {
            throw new IllegalArgumentException("subscription.extension.incomplete");
        }
    }
}

package com.arcadiadevs.viora.platform.subscription.domain.model.commands;

import java.math.BigDecimal;
/** Initial purchase request. @param producerId verified actor @param planCode offer @param requestedQuotaHa capacity @param operationId idempotency key */
public record CreateSubscriptionCommand(String producerId, String planCode, BigDecimal requestedQuotaHa, String operationId) {
    public CreateSubscriptionCommand {
        if (producerId == null || planCode == null || requestedQuotaHa == null || operationId == null) {
            throw new IllegalArgumentException("subscription.contract.invalid");
        }
    }
}

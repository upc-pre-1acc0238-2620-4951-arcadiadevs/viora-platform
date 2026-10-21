package com.arcadiadevs.viora.platform.subscription.domain.model.commands;

import java.math.BigDecimal;

/**
 * Command asking to redeem a cooperative invitation code for the authenticated producer.
 *
 * @param producerId the authenticated producer
 * @param code       the code as the producer typed it
 * @param quotaHa    the hectares the producer declared and wants covered
 */
public record RedeemCooperativeCodeCommand(String producerId, String code, BigDecimal quotaHa) {

    /**
     * Compact constructor enforcing non-null fields.
     */
    public RedeemCooperativeCodeCommand {
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
        if (code == null) {
            throw new IllegalArgumentException("subscription.invitation_code.blank");
        }
        if (quotaHa == null) {
            throw new IllegalArgumentException("subscription.quota.null");
        }
    }
}

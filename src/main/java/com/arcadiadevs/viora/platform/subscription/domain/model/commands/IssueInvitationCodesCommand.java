package com.arcadiadevs.viora.platform.subscription.domain.model.commands;

import java.math.BigDecimal;

/**
 * Command asking to issue a batch of invitation codes of the license the authenticated technical manager is in charge
 * of.
 *
 * @param managerId       the authenticated technical manager
 * @param quantity        how many codes, one per producer
 * @param hectaresPerCode the hectares each code covers
 * @param validityDays    how many days the codes can be redeemed, never past the license
 */
public record IssueInvitationCodesCommand(String managerId, Integer quantity, BigDecimal hectaresPerCode,
                                          Integer validityDays) {

    /**
     * Compact constructor enforcing non-null fields.
     */
    public IssueInvitationCodesCommand {
        if (managerId == null) {
            throw new IllegalArgumentException("subscription.manager.id.null_or_empty");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("subscription.invitation_code.batch.quantity");
        }
        if (hectaresPerCode == null) {
            throw new IllegalArgumentException("subscription.quota.null");
        }
        if (validityDays == null) {
            throw new IllegalArgumentException("subscription.invitation_code.batch.validity");
        }
    }
}

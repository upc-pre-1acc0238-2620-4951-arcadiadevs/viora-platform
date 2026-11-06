package com.arcadiadevs.viora.platform.subscription.domain.model.commands;

/**
 * Command asking to stop sharing the authenticated producer's data with the license that sponsors their plan.
 *
 * @param producerId the authenticated producer
 * @param consentId  the consent to withdraw
 */
public record WithdrawDataSharingConsentCommand(String producerId, String consentId) {

    /**
     * Compact constructor enforcing non-null fields.
     */
    public WithdrawDataSharingConsentCommand {
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
        if (consentId == null) {
            throw new IllegalArgumentException("subscription.consent.id.null_or_empty");
        }
    }
}

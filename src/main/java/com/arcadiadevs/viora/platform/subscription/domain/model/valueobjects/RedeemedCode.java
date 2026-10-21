package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/**
 * Evidence that a cooperative invitation code was redeemed by a producer. It is created only by
 * {@code CooperativeLicense.redeem}, inside the same transaction that activates the sponsored subscription.
 *
 * @param codeId        the redeemed code
 * @param licenseId     the license the code belongs to
 * @param cooperativeId the sponsoring cooperative
 * @param producerId    the producer who redeemed the code
 * @param quota         the hectares committed for the producer
 * @param licensePeriod the window of the license, which bounds the sponsored subscription
 */
public record RedeemedCode(
        InvitationCodeId codeId,
        CooperativeLicenseId licenseId,
        CooperativeId cooperativeId,
        ProducerId producerId,
        HectaresQuota quota,
        SubscriptionPeriod licensePeriod
) {

    /**
     * Validates that every part of the evidence is present.
     *
     * @throws IllegalArgumentException if any part is null
     */
    public RedeemedCode {
        if (codeId == null || licenseId == null || cooperativeId == null || producerId == null
                || quota == null || licensePeriod == null) {
            throw new IllegalArgumentException("subscription.redeemed_code.incomplete");
        }
    }
}

package com.arcadiadevs.viora.platform.subscription.domain.model.commands;

/**
 * Command asking to redeem a cooperative invitation code for the authenticated producer.
 *
 * @param producerId         the authenticated producer
 * @param code               the code as the producer typed it
 * @param consentTextVersion the version of the consent text the producer accepted to share their data with the
 *                           license
 */
public record RedeemCooperativeCodeCommand(String producerId, String code, String consentTextVersion) {

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
        if (consentTextVersion == null) {
            throw new IllegalArgumentException("subscription.consent.text_version.blank");
        }
    }
}

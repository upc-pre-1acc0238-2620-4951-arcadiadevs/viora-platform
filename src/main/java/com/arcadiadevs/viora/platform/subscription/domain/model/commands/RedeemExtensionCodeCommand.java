package com.arcadiadevs.viora.platform.subscription.domain.model.commands;

/**
 * Command asking to add the hectares of an extension code to the authenticated producer's sponsored plan.
 *
 * @param producerId the authenticated producer
 * @param code       the code as the producer typed it
 */
public record RedeemExtensionCodeCommand(String producerId, String code) {

    /**
     * Compact constructor enforcing non-null fields.
     */
    public RedeemExtensionCodeCommand {
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
        if (code == null) {
            throw new IllegalArgumentException("subscription.extension_code.blank");
        }
    }
}

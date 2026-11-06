package com.arcadiadevs.viora.platform.subscription.domain.model.commands;

/**
 * Command asking to redeem a manager activation code for the authenticated technical manager.
 *
 * @param managerId the authenticated technical manager
 * @param code      the code as the manager typed it
 */
public record RedeemManagerCodeCommand(String managerId, String code) {

    /**
     * Compact constructor enforcing non-null fields.
     */
    public RedeemManagerCodeCommand {
        if (managerId == null) {
            throw new IllegalArgumentException("subscription.manager.id.null_or_empty");
        }
        if (code == null) {
            throw new IllegalArgumentException("subscription.manager_code.blank");
        }
    }
}

package com.arcadiadevs.viora.platform.subscription.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Thrown when an extension code cannot add hectares to a producer's plan: the code does not exist, was revoked or
 * expired, belongs to another license, the producer already used it, the plan is not in force or the license has no
 * hectares left. The reason is deliberately the same for every case, so a caller cannot tell one from the other.
 */
public class ExtensionCodeNotRedeemableException extends BusinessRuleException {

    /** The i18n key of the single message shown for any code that cannot be redeemed. */
    public static final String MESSAGE_KEY = "subscription.extension_code.not_redeemable";

    /**
     * Creates the exception with the shared message key.
     */
    public ExtensionCodeNotRedeemableException() {
        super(MESSAGE_KEY);
    }
}

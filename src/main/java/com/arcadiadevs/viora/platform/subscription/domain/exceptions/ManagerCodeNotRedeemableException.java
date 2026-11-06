package com.arcadiadevs.viora.platform.subscription.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Thrown when a manager activation code cannot be redeemed: it does not exist, it was already redeemed, it expired, it
 * was revoked, or its license is not in force. The reason is the same for every case, so a caller cannot tell one from
 * the other.
 */
public class ManagerCodeNotRedeemableException extends BusinessRuleException {

    /** The i18n key of the single message shown for any code that cannot be redeemed. */
    public static final String MESSAGE_KEY = "subscription.manager_code.not_redeemable";

    /**
     * Creates the exception with the shared message key.
     */
    public ManagerCodeNotRedeemableException() {
        super(MESSAGE_KEY);
    }
}

package com.arcadiadevs.viora.platform.subscription.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Thrown when a cooperative invitation code cannot be redeemed: it was already redeemed, it expired, it was revoked,
 * or its license is no longer in force. The reason is deliberately the same for every case, so a caller cannot tell
 * one from the other.
 */
public class InvitationCodeNotRedeemableException extends BusinessRuleException {

    /** The i18n key of the single message shown for any code that cannot be redeemed. */
    public static final String MESSAGE_KEY = "subscription.invitation_code.not_redeemable";

    /**
     * Creates the exception with the shared message key.
     */
    public InvitationCodeNotRedeemableException() {
        super(MESSAGE_KEY);
    }
}

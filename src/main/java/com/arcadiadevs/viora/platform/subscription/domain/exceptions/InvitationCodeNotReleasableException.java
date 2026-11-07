package com.arcadiadevs.viora.platform.subscription.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Thrown when a member cannot be released: their code is not redeemed, or they were already released.
 */
public class InvitationCodeNotReleasableException extends BusinessRuleException {

    /** The i18n key of the message. */
    public static final String MESSAGE_KEY = "subscription.member.already_released";

    /**
     * Creates the exception with its message key.
     */
    public InvitationCodeNotReleasableException() {
        super(MESSAGE_KEY);
    }
}

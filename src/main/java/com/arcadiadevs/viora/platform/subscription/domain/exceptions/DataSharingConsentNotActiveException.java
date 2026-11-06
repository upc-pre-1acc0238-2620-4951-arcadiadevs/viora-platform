package com.arcadiadevs.viora.platform.subscription.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Thrown when a producer tries to stop sharing data that is no longer shared: they already stopped, or the sponsored
 * plan ended.
 */
public class DataSharingConsentNotActiveException extends BusinessRuleException {

    /** The i18n key of the message. */
    public static final String MESSAGE_KEY = "subscription.consent.not_active";

    /**
     * Creates the exception with its message key.
     */
    public DataSharingConsentNotActiveException() {
        super(MESSAGE_KEY);
    }
}

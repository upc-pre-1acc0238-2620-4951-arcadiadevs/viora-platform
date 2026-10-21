package com.arcadiadevs.viora.platform.subscription.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Thrown when a cooperative license has fewer hectares left than a new code would cover.
 */
public class InsufficientLicenseAreaException extends BusinessRuleException {

    /** The i18n key of the message. */
    public static final String MESSAGE_KEY = "subscription.license.insufficient_area";

    /**
     * Creates the exception with its message key.
     */
    public InsufficientLicenseAreaException() {
        super(MESSAGE_KEY);
    }
}

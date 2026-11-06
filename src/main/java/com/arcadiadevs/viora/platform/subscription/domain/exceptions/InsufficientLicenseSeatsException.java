package com.arcadiadevs.viora.platform.subscription.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Thrown when a batch of invitation codes needs more seats than the license has free: each code still to be redeemed
 * and each redeemed one holds a seat.
 */
public class InsufficientLicenseSeatsException extends BusinessRuleException {

    /** The i18n key of the message. */
    public static final String MESSAGE_KEY = "subscription.license.insufficient_seats";

    /**
     * Creates the exception with its message key.
     */
    public InsufficientLicenseSeatsException() {
        super(MESSAGE_KEY);
    }
}

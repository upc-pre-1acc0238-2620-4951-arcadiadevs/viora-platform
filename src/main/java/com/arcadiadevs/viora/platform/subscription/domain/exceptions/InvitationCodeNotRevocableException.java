package com.arcadiadevs.viora.platform.subscription.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

import java.time.Instant;

/**
 * Thrown when an invitation code can no longer be revoked: a producer redeemed it, or it expired or was revoked
 * before. A redeemed code tells when, so the manager can see it was used.
 */
public class InvitationCodeNotRevocableException extends BusinessRuleException {

    /** The i18n key of a code a producer already redeemed. */
    public static final String ALREADY_REDEEMED_KEY = "subscription.invitation_code.already_redeemed";
    /** The i18n key of a code that expired or was revoked. */
    public static final String NOT_AVAILABLE_KEY = "subscription.invitation_code.not_available";

    private final transient Instant redeemedAt;

    private InvitationCodeNotRevocableException(String messageKey, Instant redeemedAt) {
        super(messageKey);
        this.redeemedAt = redeemedAt;
    }

    /**
     * Creates the exception for a code a producer already redeemed.
     *
     * @param redeemedAt when it was redeemed
     * @return the exception
     */
    public static InvitationCodeNotRevocableException alreadyRedeemed(Instant redeemedAt) {
        return new InvitationCodeNotRevocableException(ALREADY_REDEEMED_KEY, redeemedAt);
    }

    /**
     * Creates the exception for a code that expired or was revoked.
     *
     * @return the exception
     */
    public static InvitationCodeNotRevocableException notAvailable() {
        return new InvitationCodeNotRevocableException(NOT_AVAILABLE_KEY, null);
    }

    /**
     * Returns when the code was redeemed.
     *
     * @return the instant, or null if the code was not redeemed
     */
    public Instant redeemedAt() {
        return redeemedAt;
    }
}

package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/**
 * The groups a manager sees the codes of their license in: the ones still to redeem, the redeemed ones, and the closed
 * ones, which expired or were revoked.
 */
public enum InvitationCodeFilter {
    AVAILABLE,
    REDEEMED,
    CLOSED;

    /**
     * Tells whether a code with a status falls in this group.
     *
     * @param status the status of the code at the moment it is shown
     * @return true if the code belongs to this group
     */
    public boolean includes(InvitationCodeStatus status) {
        return switch (this) {
            case AVAILABLE -> status == InvitationCodeStatus.AVAILABLE;
            case REDEEMED -> status == InvitationCodeStatus.REDEEMED;
            case CLOSED -> status == InvitationCodeStatus.EXPIRED || status == InvitationCodeStatus.REVOKED;
        };
    }
}

package com.arcadiadevs.viora.platform.subscription.domain.model.commands;

/**
 * Command asking to revoke an invitation code of the license the authenticated technical manager is in charge of.
 *
 * @param managerId        the authenticated technical manager
 * @param invitationCodeId the code to revoke
 */
public record RevokeInvitationCodeCommand(String managerId, String invitationCodeId) {

    /**
     * Compact constructor enforcing non-null fields.
     */
    public RevokeInvitationCodeCommand {
        if (managerId == null) {
            throw new IllegalArgumentException("subscription.manager.id.null_or_empty");
        }
        if (invitationCodeId == null) {
            throw new IllegalArgumentException("subscription.invitation_code.id.null_or_empty");
        }
    }
}

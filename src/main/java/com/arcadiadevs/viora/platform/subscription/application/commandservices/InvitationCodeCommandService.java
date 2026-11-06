package com.arcadiadevs.viora.platform.subscription.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.IssueInvitationCodesCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RevokeInvitationCodeCommand;


/**
 * Command service port of the invitation codes a technical manager issues for the producers of their license.
 */
public interface InvitationCodeCommandService {

    /**
     * Issues a batch of codes, each holding a seat and its hectares of the manager's license.
     *
     * @param command the batch request
     * @return the new codes, readable in full, or the reason they were not issued
     */
    Result<InvitationCodeBatch, ApplicationError> handle(IssueInvitationCodesCommand command);

    /**
     * Revokes a code nobody redeemed yet, giving its seat and hectares back to the license.
     *
     * @param command the revocation request
     * @return the revoked code, or the reason it was not revoked
     */
    Result<InvitationCode, ApplicationError> handle(RevokeInvitationCodeCommand command);
}

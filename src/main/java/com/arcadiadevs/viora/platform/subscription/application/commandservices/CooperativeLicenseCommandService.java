package com.arcadiadevs.viora.platform.subscription.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.ReleaseCooperativeMemberCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RenewCooperativeLicenseCommand;

/**
 * Command service port of the cooperative licenses: releasing their members and renewing them.
 */
public interface CooperativeLicenseCommandService {

    /**
     * Releases a member from the manager's license.
     *
     * @param command the manager, the license and the member
     * @return what the release did, or why it was not done
     */
    Result<MemberRelease, ApplicationError> handle(ReleaseCooperativeMemberCommand command);

    /**
     * Renews a license to a later end; its members' plans follow without new codes.
     *
     * @param command the license and its new end
     * @return the renewed license, or why it was not renewed
     */
    Result<CooperativeLicense, ApplicationError> handle(RenewCooperativeLicenseCommand command);
}

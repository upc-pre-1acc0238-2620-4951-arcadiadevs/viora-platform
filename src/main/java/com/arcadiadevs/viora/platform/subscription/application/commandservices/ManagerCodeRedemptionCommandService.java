package com.arcadiadevs.viora.platform.subscription.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemManagerCodeCommand;

/**
 * Command service port for redeeming manager activation codes.
 */
public interface ManagerCodeRedemptionCommandService {

    /**
     * Redeems a manager activation code and puts the technical manager in charge of its license.
     *
     * @param command the redemption request
     * @return the license the manager is now in charge of, or the reason the code was not redeemed
     */
    Result<CooperativeLicense, ApplicationError> handle(RedeemManagerCodeCommand command);
}

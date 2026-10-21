package com.arcadiadevs.viora.platform.subscription.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemCooperativeCodeCommand;

/**
 * Command service port for redeeming cooperative invitation codes.
 */
public interface CooperativeCodeRedemptionCommandService {

    /**
     * Redeems a cooperative invitation code for a producer and activates the sponsored subscription.
     *
     * @param command the redemption request
     * @return the active sponsored subscription, or the reason the code was not redeemed
     */
    Result<Subscription, ApplicationError> handle(RedeemCooperativeCodeCommand command);
}

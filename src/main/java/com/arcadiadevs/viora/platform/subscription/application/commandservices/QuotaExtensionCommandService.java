package com.arcadiadevs.viora.platform.subscription.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemExtensionCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RequestQuotaExtensionCommand;

/**
 * Application boundary for growing a plan in force: paid hectares for an individual plan, or hectares from an extension
 * code for a sponsored plan.
 */
public interface QuotaExtensionCommandService {

    /**
     * Requests paid hectares for an individual plan, or replays the same request; nothing is added until the payment
     * is approved.
     *
     * @param command the authenticated request
     * @return the pending extension, flagged when replayed, or a consistent failure
     */
    Result<RequestedExtension, ApplicationError> handle(RequestQuotaExtensionCommand command);

    /**
     * Adds the hectares of an extension code to the producer's sponsored plan.
     *
     * @param command the authenticated redemption
     * @return the applied extension, or a consistent failure
     */
    Result<QuotaExtension, ApplicationError> handle(RedeemExtensionCodeCommand command);

    /**
     * A paid extension request.
     *
     * @param extension the pending extension
     * @param replayed  whether the same operation had already created it
     */
    record RequestedExtension(QuotaExtension extension, boolean replayed) { }
}

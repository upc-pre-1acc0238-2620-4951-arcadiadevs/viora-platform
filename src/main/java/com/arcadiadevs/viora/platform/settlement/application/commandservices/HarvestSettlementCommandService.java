package com.arcadiadevs.viora.platform.settlement.application.commandservices;

import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SettleCampaignHarvestCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/**
 * Use case: settle the delivered harvest of a campaign.
 *
 * <p>The outcome carries whether the settlement was created or replayed from an idempotency key, so the caller can
 * answer 201 or 200.</p>
 */
public interface HarvestSettlementCommandService {

    /**
     * Settles the delivered harvest of a campaign.
     *
     * @param command the validated instruction to settle
     * @return the settlement created or replayed, or the failure that prevented it
     */
    Result<SettlementOutcome, ApplicationError> handle(SettleCampaignHarvestCommand command);
}

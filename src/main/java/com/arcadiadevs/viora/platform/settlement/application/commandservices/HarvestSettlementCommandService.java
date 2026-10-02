package com.arcadiadevs.viora.platform.settlement.application.commandservices;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SettleCampaignHarvestCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/** Use case: settle the delivered harvest of a campaign. */
public interface HarvestSettlementCommandService {

    Result<HarvestSettlementSnapshot, ApplicationError> handle(SettleCampaignHarvestCommand command);
}

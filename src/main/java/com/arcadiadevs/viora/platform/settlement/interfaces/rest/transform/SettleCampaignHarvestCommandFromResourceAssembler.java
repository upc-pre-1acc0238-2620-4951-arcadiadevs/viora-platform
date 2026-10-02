package com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SettleCampaignHarvestCommand;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.SettleHarvestResource;

/** Maps HTTP input to the validated settlement command. */
public final class SettleCampaignHarvestCommandFromResourceAssembler {
    private SettleCampaignHarvestCommandFromResourceAssembler() { }

    public static SettleCampaignHarvestCommand toCommand(String plotId, String actorId, SettleHarvestResource resource) {
        return new SettleCampaignHarvestCommand(plotId, actorId, resource.campaignYear(), resource.greenOlivesKg(),
                resource.blackOlivesKg(), resource.commercialFruitsPerKg(), resource.notes());
    }
}

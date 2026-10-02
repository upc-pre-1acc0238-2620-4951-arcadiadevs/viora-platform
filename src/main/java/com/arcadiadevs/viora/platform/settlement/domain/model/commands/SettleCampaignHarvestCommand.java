package com.arcadiadevs.viora.platform.settlement.domain.model.commands;

import com.arcadiadevs.viora.platform.settlement.domain.model.entities.HarvestSettlement;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;

/**
 * Validated instruction to settle the delivered harvest of a campaign.
 *
 * @param plotId                plot to settle
 * @param actorId               producer performing the settlement
 * @param campaignYear          campaign, 2000 to 2100
 * @param greenOlivesKg         green olives delivered, kilograms
 * @param blackOlivesKg         black olives delivered, kilograms
 * @param commercialFruitsPerKg optional caliber of the delivered olives
 * @param notes                 optional notes
 */
public record SettleCampaignHarvestCommand(String plotId, String actorId, Integer campaignYear,
        Double greenOlivesKg, Double blackOlivesKg, Double commercialFruitsPerKg, String notes) {

    public SettleCampaignHarvestCommand {
        plotId = new PlotId(plotId).plotId();
        actorId = new UserId(actorId).userId();
        new CampaignYear(campaignYear);
        HarvestSettlement.calculateTotalWeight(new OliveWeight(greenOlivesKg), new OliveWeight(blackOlivesKg));
        if (commercialFruitsPerKg != null
                && (!Double.isFinite(commercialFruitsPerKg) || commercialFruitsPerKg <= 0.0)) {
            throw new IllegalArgumentException("settlement.fruits_per_kg.invalid");
        }
        if (notes != null && notes.length() > HarvestSettlement.MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("settlement.notes.too_long");
        }
    }
}

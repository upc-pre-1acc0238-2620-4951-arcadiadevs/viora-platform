package com.arcadiadevs.viora.platform.settlement.domain.model.commands;

import com.arcadiadevs.viora.platform.settlement.domain.model.entities.HarvestSettlement;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;

import java.time.LocalDate;

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
 * @param weighedOn             date the delivered olives were weighed; whether it lies in the future is decided by
 *                              {@link WeighingDate#of(LocalDate, java.time.Clock)}, which owns that rule
 * @param millTicketNumber      optional ticket number of the receiving mill, trimmed; blank becomes absent
 * @param idempotencyKey        optional key making the settlement replayable, trimmed; blank becomes absent
 */
public record SettleCampaignHarvestCommand(String plotId, String actorId, Integer campaignYear,
        Double greenOlivesKg, Double blackOlivesKg, Double commercialFruitsPerKg, String notes,
        LocalDate weighedOn, String millTicketNumber, String idempotencyKey) {

    /**
     * Validates the instruction and normalises the two optional identifiers, so a header or body value that is too
     * long is rejected as a validation error before any transaction touches the database.
     *
     * @throws IllegalArgumentException if the plot, the producer or the campaign is invalid, if the weights do not
     *                                  add up to a positive total, if the caliber is invalid, if the notes exceed
     *                                  {@link HarvestSettlement#MAX_NOTES_LENGTH} characters, or if the mill ticket
     *                                  or the idempotency key exceeds its own limit
     */
    public SettleCampaignHarvestCommand {
        plotId = new PlotId(plotId).plotId();
        actorId = new UserId(actorId).userId();
        new CampaignYear(campaignYear);
        HarvestSettlement.validateFigures(new OliveWeight(greenOlivesKg), new OliveWeight(blackOlivesKg),
                commercialFruitsPerKg, notes);
        millTicketNumber = MillTicketNumber.of(millTicketNumber).value();
        idempotencyKey = IdempotencyKey.of(idempotencyKey).value();
    }
}

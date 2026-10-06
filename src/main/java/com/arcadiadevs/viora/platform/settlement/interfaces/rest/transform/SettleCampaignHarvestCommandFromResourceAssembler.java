package com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SettleCampaignHarvestCommand;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.SettleHarvestResource;

/** Maps HTTP input to the validated settlement command. */
public final class SettleCampaignHarvestCommandFromResourceAssembler {
    private SettleCampaignHarvestCommandFromResourceAssembler() { }

    /**
     * Builds the settlement command from the request body and the optional idempotency key header.
     *
     * <p>An idempotency key longer than its limit is rejected here, by the command, which surfaces it as a
     * validation error of the request, exactly like the plot and the campaign of the route.</p>
     *
     * @param plotId         plot taken from the route
     * @param actorId        producer performing the settlement
     * @param idempotencyKey optional {@code Idempotency-Key} header, may be null
     * @param resource       the request body
     * @return the validated command
     * @throws IllegalArgumentException if the plot, the producer, the campaign, the weights, the caliber, the
     *                                  notes, the mill ticket or the idempotency key are invalid
     */
    public static SettleCampaignHarvestCommand toCommand(String plotId, String actorId, String idempotencyKey,
            SettleHarvestResource resource) {
        return new SettleCampaignHarvestCommand(plotId, actorId, resource.campaignYear(), resource.greenOlivesKg(),
                resource.blackOlivesKg(), resource.commercialFruitsPerKg(), resource.notes(),
                resource.weighedOn(), resource.millTicketNumber(), idempotencyKey);
    }
}

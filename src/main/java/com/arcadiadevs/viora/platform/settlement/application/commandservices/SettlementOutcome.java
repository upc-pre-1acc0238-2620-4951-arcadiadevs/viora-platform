package com.arcadiadevs.viora.platform.settlement.application.commandservices;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;

/**
 * Outcome of settling a campaign: the settlement voucher plus whether this call is the one that created it.
 *
 * <p>A caller that replays a request with an idempotency key gets the very same voucher back, but it must answer
 * 200 instead of 201 because nothing was created. The flag is the only way for the REST layer to tell both apart.</p>
 *
 * @param settlement the settlement the call resolved to, either freshly created or replayed
 * @param created    {@code true} when this call created the settlement, {@code false} when it replayed it
 */
public record SettlementOutcome(HarvestSettlementSnapshot settlement, boolean created) {

    /**
     * Builds the outcome of a settlement that was just created.
     *
     * @param settlement the created settlement
     * @return the outcome, flagged as created
     */
    public static SettlementOutcome created(HarvestSettlementSnapshot settlement) {
        return new SettlementOutcome(settlement, true);
    }

    /**
     * Builds the outcome of a settlement that already existed and has been replayed.
     *
     * @param settlement the replayed settlement
     * @return the outcome, flagged as replayed
     */
    public static SettlementOutcome replayed(HarvestSettlementSnapshot settlement) {
        return new SettlementOutcome(settlement, false);
    }
}

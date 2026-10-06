package com.arcadiadevs.viora.platform.settlement.domain.repositories;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.IdempotencyKey;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;

import java.util.Optional;

/**
 * Domain port that reaches a settlement directly, without going through the report of a plot.
 *
 * <p>Settlements are children of an agronomic report, so they are normally read plot by plot. An idempotent replay
 * needs the opposite: a producer may retry from any of their plots, so the settlement has to be found by the
 * producer and the idempotency key it was registered with.</p>
 *
 * <p>There is deliberately no lookup by producer and campaign year: a producer may settle one campaign on several
 * plots, so that pair does not identify a single settlement.</p>
 */
public interface SettledHarvestRepository {

    /**
     * Finds the settlement a producer registered with an idempotency key, across all of their plots.
     *
     * @param producerId     producer that owns the settlement
     * @param idempotencyKey key the settlement was registered with
     * @return the settlement, empty when the producer has no settlement under that key
     */
    Optional<HarvestSettlementSnapshot> findByProducerIdAndIdempotencyKey(UserId producerId,
            IdempotencyKey idempotencyKey);

    /**
     * Finds the highest receipt sequence already issued to a producer within a campaign year, across all of their
     * plots.
     *
     * @param producerId   producer that owns the settlements
     * @param campaignYear campaign the receipt numbers belong to
     * @return the highest sequence found, zero when no settlement of the pair carries a receipt number
     */
    int findHighestReceiptSequence(UserId producerId, CampaignYear campaignYear);
}

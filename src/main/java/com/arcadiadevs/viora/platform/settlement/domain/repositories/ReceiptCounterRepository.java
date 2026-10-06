package com.arcadiadevs.viora.platform.settlement.domain.repositories;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.ReceiptCounter;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;

import java.util.Optional;

/** Domain port for the receipt number counters that number the settlements of a producer per campaign year. */
public interface ReceiptCounterRepository {

    /**
     * Makes sure the counter of a producer and campaign exists, opening it at sequence zero when it does not.
     *
     * <p>Must be idempotent and must never fail the calling transaction when a concurrent settlement opened the
     * same counter first, because that settlement is a legitimate race rather than an error.</p>
     *
     * @param producerId  producer the numbers belong to
     * @param campaignYear campaign the numbers belong to
     */
    void ensureExists(UserId producerId, CampaignYear campaignYear);

    /**
     * Loads and locks the counter of a producer and campaign until the caller's transaction completes, so the
     * sequence it hands out is not consumed twice.
     *
     * @param producerId  producer the numbers belong to
     * @param campaignYear campaign the numbers belong to
     * @return the locked counter, empty when {@link #ensureExists} was not called first
     */
    Optional<ReceiptCounter> findByProducerIdAndCampaignYearForUpdate(UserId producerId, CampaignYear campaignYear);

    /**
     * Persists the counter with the sequences it has just handed out.
     *
     * @param counter counter to persist
     */
    void save(ReceiptCounter counter);
}

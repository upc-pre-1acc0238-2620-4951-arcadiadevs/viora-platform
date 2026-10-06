package com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.ReceiptCounterRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opens the receipt counter of a producer and campaign before a settlement locks it.
 *
 * <p>The counter row does not exist the first time a producer settles a campaign, so it has to be created. Two
 * producers, or two plots of the same producer, settling the same campaign at the same time both try to create it,
 * and the loser of that race must not fail: its transaction would end up aborted, because Postgres keeps a
 * transaction unusable once a constraint has been violated. That is why the creation is not a plain insert that the
 * caller catches, and why it is not a separate transaction either.</p>
 *
 * <p>Instead the repository opens the counter with a single atomic statement that leaves an already existing
 * counter untouched, so nothing is ever violated and nothing has to be swallowed. It runs in the caller's
 * transaction on purpose: a {@link Propagation#REQUIRES_NEW} one would need a second pooled connection while the
 * settlement still holds the first, and the pool is deliberately capped (see {@code DB_MAX_POOL_SIZE}), which would
 * starve as soon as two settlements ran at the same time. {@link Propagation#REQUIRED} therefore only joins the
 * transaction of the settlement: the counter row is opened with it, and committed or rolled back with it, so it
 * provides no isolation of its own.</p>
 *
 * <p>The counter is opened at sequence zero. Bringing it up to the receipt numbers already issued is not done here
 * but by the settlement, once it holds the lock of the row.</p>
 */
@Component
public class ReceiptCounterInitializer {

    private final ReceiptCounterRepository counterRepository;

    /**
     * Constructs the initializer.
     *
     * @param counterRepository domain port of the receipt counters
     */
    public ReceiptCounterInitializer(ReceiptCounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    /**
     * Makes sure the counter of a producer and campaign exists, so it can be locked and moved forward afterwards.
     *
     * @param producerId  producer whose receipt numbers are being numbered
     * @param campaignYear campaign the numbers belong to
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void ensureExists(UserId producerId, CampaignYear campaignYear) {
        counterRepository.ensureExists(producerId, campaignYear);
    }
}

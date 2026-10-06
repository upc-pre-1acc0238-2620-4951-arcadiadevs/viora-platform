package com.arcadiadevs.viora.platform.settlement.domain.model.aggregates;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ReceiptNumber;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;

/**
 * Counter of the receipt numbers a producer has consumed within one campaign year.
 *
 * <p>Receipt numbers are numbered per producer, not per plot: two different plots of the same producer settling the
 * same campaign must never receive the same number. The plot lock held while settling only serializes the plots of
 * one plot, so the numbering needs its own aggregate, its own row and its own lock.</p>
 *
 * <p>{@link #nextSequence()} is the only way to move the counter forward and {@link #save} is the only way to make
 * the move durable, so a sequence is consumed inside a transaction that either commits the counter or rolls it
 * back. There are no setters: a counter only moves by handing out a number.</p>
 */
public final class ReceiptCounter {

    private final UserId producerId;
    private final CampaignYear campaignYear;
    private int lastSequence;

    private ReceiptCounter(UserId producerId, CampaignYear campaignYear, int lastSequence) {
        this.producerId = producerId;
        this.campaignYear = campaignYear;
        this.lastSequence = lastSequence;
    }

    /**
     * Opens the counter of a producer and campaign, before any receipt number has been handed out.
     *
     * @param producerId  producer the numbers belong to
     * @param campaignYear campaign the numbers belong to
     * @return a counter whose last sequence is zero
     * @throws IllegalArgumentException if the producer or the campaign is missing
     */
    public static ReceiptCounter start(UserId producerId, CampaignYear campaignYear) {
        return reconstitute(producerId, campaignYear, 0);
    }

    /**
     * Rebuilds a counter from persistence.
     *
     * @param producerId   producer the numbers belong to
     * @param campaignYear campaign the numbers belong to
     * @param lastSequence last sequence already handed out, zero or positive
     * @return the rebuilt counter
     * @throws IllegalArgumentException if the producer or the campaign is missing, or if the last sequence is negative
     */
    public static ReceiptCounter reconstitute(UserId producerId, CampaignYear campaignYear, int lastSequence) {
        if (producerId == null || campaignYear == null) {
            throw new IllegalArgumentException("settlement.reference.null");
        }
        if (lastSequence < 0) {
            throw new IllegalArgumentException("settlement.receipt_counter.sequence.invalid");
        }
        return new ReceiptCounter(producerId, campaignYear, lastSequence);
    }

    /**
     * Moves the counter up to the highest sequence already issued in the settlements, never down.
     *
     * <p>The counter is the source of truth for new numbers, but receipt numbers also exist outside of it: rows
     * numbered by a backfill, or by a counter that was lost or opened empty. Catching up before consuming a number
     * guarantees a number that is already on a receipt is never handed out again.</p>
     *
     * @param highestIssuedSequence highest sequence found in the stored receipt numbers, zero when there are none
     */
    public void catchUpTo(int highestIssuedSequence) {
        if (highestIssuedSequence > lastSequence) {
            lastSequence = highestIssuedSequence;
        }
    }

    /**
     * Consumes the next number of the counter.
     *
     * <p>Must be called while the counter row is locked, so two settlements of the same producer and campaign never
     * consume the same number.</p>
     *
     * @return the sequence just consumed, starting at one
     */
    public int nextSequence() {
        lastSequence += 1;
        return lastSequence;
    }

    /**
     * Mints the receipt number of the next sequence of this counter.
     *
     * @return the receipt number of the next sequence within the campaign year
     */
    public ReceiptNumber nextReceiptNumber() {
        return ReceiptNumber.of(campaignYear, nextSequence());
    }

    /**
     * @return the producer the numbers belong to
     */
    public UserId producerId() {
        return producerId;
    }

    /**
     * @return the campaign the numbers belong to
     */
    public CampaignYear campaignYear() {
        return campaignYear;
    }

    /**
     * @return the last sequence already handed out, zero when the counter was just opened
     */
    public int lastSequence() {
        return lastSequence;
    }
}

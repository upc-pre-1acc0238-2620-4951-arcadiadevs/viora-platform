package com.arcadiadevs.viora.platform.settlement.domain.model.entities;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;

import java.time.Instant;

/** Formal, immutable settlement of the delivered harvest of one campaign. */
public final class HarvestSettlement {

    public static final int MAX_NOTES_LENGTH = 1000;

    private final HarvestSettlementSnapshot state;

    private HarvestSettlement(HarvestSettlementSnapshot state) {
        this.state = state;
    }

    /**
     * Validates the weights of a campaign and returns their total.
     *
     * @param green green olives delivered
     * @param black black olives delivered
     * @return green plus black, strictly positive
     */
    public static OliveWeight calculateTotalWeight(OliveWeight green, OliveWeight black) {
        if (green == null || black == null) {
            throw new IllegalArgumentException("settlement.weight.invalid");
        }
        var total = green.plus(black);
        if (total.kilograms() <= 0.0) {
            throw new IllegalArgumentException("settlement.weight.total.not_positive");
        }
        return total;
    }

    /**
     * Checks the figures of a settlement that decide whether it can be registered at all: the weights, the caliber
     * and the notes.
     *
     * <p>{@link #create} runs exactly these checks. Exposing them lets a caller that has to consume something
     * scarce before registering (a receipt number) reject an invalid request first instead of burning the number.</p>
     *
     * @param green                 green olives delivered
     * @param black                 black olives delivered
     * @param commercialFruitsPerKg optional caliber of the delivered olives
     * @param notes                 optional notes
     * @return the strictly positive total weight
     * @throws IllegalArgumentException if the weights do not add up to a positive total, or if the caliber or the
     *                                  notes are invalid
     */
    public static OliveWeight validateFigures(OliveWeight green, OliveWeight black, Double commercialFruitsPerKg,
            String notes) {
        if (commercialFruitsPerKg != null
                && (!Double.isFinite(commercialFruitsPerKg) || commercialFruitsPerKg <= 0.0)) {
            throw new IllegalArgumentException("settlement.fruits_per_kg.invalid");
        }
        if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("settlement.notes.too_long");
        }
        return calculateTotalWeight(green, black);
    }

    /**
     * Registers a settlement of one campaign, freezing its receipt and weighing data with the rest of the voucher.
     *
     * @param reportId             parent agronomic report
     * @param plotId               settled plot
     * @param campaignYear         settled campaign
     * @param green                green olives delivered
     * @param black                black olives delivered
     * @param commercialFruitsPerKg optional caliber of the delivered olives
     * @param notes                optional notes
     * @param receiptNumber        official receipt number allocated to this settlement
     * @param weighedOn            date the delivered olives were weighed
     * @param millTicketNumber     optional ticket number of the receiving mill
     * @param idempotencyKey       optional key the settlement is registered with
     * @param thinningBalance      balance against the thinning prescription
     * @param trendCurve           stabilization curve of the plot
     * @param settledAt            instant of the settlement
     * @return the settlement entity wrapping its snapshot
     * @throws IllegalArgumentException if a mandatory reference is missing, if the caliber or the notes are invalid,
     *                                  or if the weights do not add up to a positive total
     */
    public static HarvestSettlement create(ReportId reportId, PlotId plotId, CampaignYear campaignYear,
            OliveWeight green, OliveWeight black, Double commercialFruitsPerKg, String notes,
            ReceiptNumber receiptNumber, WeighingDate weighedOn, MillTicketNumber millTicketNumber,
            IdempotencyKey idempotencyKey, ThinningBalance thinningBalance, StabilizationTrendCurve trendCurve,
            Instant settledAt) {
        if (reportId == null || plotId == null || campaignYear == null || thinningBalance == null
                || trendCurve == null || settledAt == null || receiptNumber == null || weighedOn == null) {
            throw new IllegalArgumentException("settlement.reference.null");
        }
        var total = validateFigures(green, black, commercialFruitsPerKg, notes);
        return new HarvestSettlement(new HarvestSettlementSnapshot(new SettlementId(), reportId, plotId,
                campaignYear, green, black, total, commercialFruitsPerKg, notes, SettlementStatus.SETTLED,
                settledAt, thinningBalance, trendCurve, receiptNumber, weighedOn, millTicketNumber,
                idempotencyKey));
    }

    public HarvestSettlementSnapshot snapshot() {
        return state;
    }
}

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

    public static HarvestSettlement create(ReportId reportId, PlotId plotId, CampaignYear campaignYear,
            OliveWeight green, OliveWeight black, Double commercialFruitsPerKg, String notes,
            ThinningBalance thinningBalance, StabilizationTrendCurve trendCurve, Instant settledAt) {
        if (reportId == null || plotId == null || campaignYear == null || thinningBalance == null
                || trendCurve == null || settledAt == null) {
            throw new IllegalArgumentException("settlement.reference.null");
        }
        if (commercialFruitsPerKg != null
                && (!Double.isFinite(commercialFruitsPerKg) || commercialFruitsPerKg <= 0.0)) {
            throw new IllegalArgumentException("settlement.fruits_per_kg.invalid");
        }
        if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("settlement.notes.too_long");
        }
        var total = calculateTotalWeight(green, black);
        return new HarvestSettlement(new HarvestSettlementSnapshot(new SettlementId(), reportId, plotId,
                campaignYear, green, black, total, commercialFruitsPerKg, notes, SettlementStatus.SETTLED,
                settledAt, thinningBalance, trendCurve));
    }

    public HarvestSettlementSnapshot snapshot() {
        return state;
    }
}

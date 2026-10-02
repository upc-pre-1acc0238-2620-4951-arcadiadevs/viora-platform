package com.arcadiadevs.viora.platform.settlement.domain.model.aggregates;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;

import java.time.Instant;

/**
 * Immutable voucher of an annual settlement, including the identity of its parent report and plot.
 *
 * @param id                    settlement identifier
 * @param reportId              parent agronomic report
 * @param plotId                settled plot
 * @param campaignYear          settled campaign
 * @param greenOlivesWeight     green olives delivered
 * @param blackOlivesWeight     black olives delivered
 * @param totalHarvestWeight    green plus black
 * @param commercialFruitsPerKg optional caliber of the delivered table olives (fruits per kilogram)
 * @param notes                 optional settlement notes
 * @param status                settlement status
 * @param settledAt             instant of the settlement
 * @param thinningBalance       balance against the thinning prescription, frozen at settlement
 * @param trendCurve            stabilization curve, frozen at settlement
 */
public record HarvestSettlementSnapshot(
        SettlementId id,
        ReportId reportId,
        PlotId plotId,
        CampaignYear campaignYear,
        OliveWeight greenOlivesWeight,
        OliveWeight blackOlivesWeight,
        OliveWeight totalHarvestWeight,
        Double commercialFruitsPerKg,
        String notes,
        SettlementStatus status,
        Instant settledAt,
        ThinningBalance thinningBalance,
        StabilizationTrendCurve trendCurve
) {
}

package com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.StabilizationTrendCurve;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ThinningBalance;
import com.arcadiadevs.viora.platform.settlement.domain.services.StabilizationCurveCalculatorService;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.HarvestSettlementResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.StabilizationCurveResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.ThinningBalanceResource;

/** Maps a settlement voucher to the public response; values are rounded only here, for display. */
public final class HarvestSettlementResourceFromEntityAssembler {
    private HarvestSettlementResourceFromEntityAssembler() { }

    public static HarvestSettlementResource toResource(HarvestSettlementSnapshot settlement) {
        return new HarvestSettlementResource(settlement.id().settlementId(), settlement.reportId().reportId(),
                settlement.plotId().plotId(), settlement.campaignYear().value(),
                settlement.greenOlivesWeight().kilograms(), settlement.blackOlivesWeight().kilograms(),
                settlement.totalHarvestWeight().kilograms(), settlement.commercialFruitsPerKg(), settlement.notes(),
                settlement.status().name(), settlement.settledAt(),
                toResource(settlement.thinningBalance()), toResource(settlement.trendCurve()));
    }

    private static ThinningBalanceResource toResource(ThinningBalance balance) {
        return new ThinningBalanceResource(balance.status().name(), balance.executedDate(),
                balance.prescribedRemovalPercentage(), balance.actualRemovalPercentage(),
                round(balance.deviationPercentagePoints(), 2));
    }

    private static StabilizationCurveResource toResource(StabilizationTrendCurve curve) {
        return new StabilizationCurveResource(curve.status().name(), curve.baselineCampaigns(),
                curve.settledCampaigns(), round(curve.baselineYieldKg(), 2),
                round(curve.baselineAlternationIndex(), 4), round(curve.managedAlternationIndex(), 4),
                round(curve.amplitudeReductionRate(), 4), curve.targetAchieved(),
                round(curve.interannualVarianceKg2(), 2), round(curve.coefficientOfVariation(), 4),
                StabilizationCurveCalculatorService.MIN_CONSECUTIVE_PAIRS);
    }

    private static Double round(Double value, int decimals) {
        if (value == null) {
            return null;
        }
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}

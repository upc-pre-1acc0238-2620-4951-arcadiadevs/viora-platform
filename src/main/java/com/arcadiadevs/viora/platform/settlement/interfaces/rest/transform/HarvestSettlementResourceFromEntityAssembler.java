package com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.settlement.application.acl.CommercialSizeGradeQueryService;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.StabilizationTrendCurve;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ThinningBalance;
import com.arcadiadevs.viora.platform.settlement.domain.services.StabilizationCurveCalculatorService;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.HarvestSettlementResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.StabilizationCurveResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.ThinningBalanceResource;
import org.springframework.stereotype.Component;

/**
 * Maps a settlement voucher to the public response; values are rounded only here, for display.
 *
 * <p>The commercial size grade of the settled caliber is owned by Thinning, so it is read through the published
 * application port instead of importing anything from that context. An absent caliber stays absent.</p>
 */
@Component
public class HarvestSettlementResourceFromEntityAssembler {
    private final CommercialSizeGradeQueryService commercialSizeGradeQueryService;

    /**
     * Constructs the assembler.
     *
     * @param commercialSizeGradeQueryService port naming the commercial size grade of a caliber
     */
    public HarvestSettlementResourceFromEntityAssembler(CommercialSizeGradeQueryService commercialSizeGradeQueryService) {
        this.commercialSizeGradeQueryService = commercialSizeGradeQueryService;
    }

    /**
     * Maps a settlement voucher to its public response.
     *
     * @param settlement the settlement to render
     * @return the public resource of the settlement
     */
    public HarvestSettlementResource toResource(HarvestSettlementSnapshot settlement) {
        return new HarvestSettlementResource(settlement.id().settlementId(), settlement.reportId().reportId(),
                settlement.plotId().plotId(), settlement.campaignYear().value(),
                settlement.greenOlivesWeight().kilograms(), settlement.blackOlivesWeight().kilograms(),
                settlement.totalHarvestWeight().kilograms(), settlement.commercialFruitsPerKg(), settlement.notes(),
                settlement.status().name(), settlement.settledAt(),
                toResource(settlement.thinningBalance()), toResource(settlement.trendCurve()),
                settlement.receiptNumber() == null ? null : settlement.receiptNumber().value(),
                settlement.weighedOn() == null ? null : settlement.weighedOn().value(),
                settlement.millTicketNumber() == null ? null : settlement.millTicketNumber().value(),
                commercialSizeGradeQueryService.gradeOf(settlement.commercialFruitsPerKg()).orElse(null));
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

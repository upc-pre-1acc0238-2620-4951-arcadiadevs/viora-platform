package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningOutcomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.CampaignThinningPlan;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Outbound ACL service used by the assistant to read how far the thinning prescription of a plot got and the outcome
 * of its confirmed thinning.
 */
@Service("assistantExternalThinningService")
public class ExternalThinningService {

    private final ThinningContextFacade thinningContextFacade;

    public ExternalThinningService(ThinningContextFacade thinningContextFacade) {
        this.thinningContextFacade = thinningContextFacade;
    }

    /**
     * Resolves the prescription availability of a plot campaign.
     *
     * @param plotId       the assistant PlotId value object
     * @param campaignYear the campaign year
     * @return the availability, or empty when the plot was never sampled that campaign
     */
    public Optional<PrescriptionAvailability> findPrescriptionAvailability(PlotId plotId, int campaignYear) {
        return thinningContextFacade.findPrescriptionAvailability(plotId.plotId(), campaignYear);
    }

    /**
     * Resolves the crop load, target and window of the prescription of a plot campaign.
     *
     * @param plotId       the assistant PlotId value object
     * @param campaignYear the campaign year
     * @return the plan, or empty when the plot was never sampled that campaign
     */
    public Optional<CampaignThinningPlan> findCampaignThinningPlan(PlotId plotId, int campaignYear) {
        return thinningContextFacade.findCampaignThinningPlan(plotId.plotId(), campaignYear);
    }

    /**
     * Resolves the thinning plan of a plot campaign with the sampling, the confirmed thinning and the caliber
     * projection the screens about one plot explain.
     *
     * @param plotId       the assistant PlotId value object
     * @param campaignYear the campaign year
     * @return the plan, or empty when the plot was never sampled that campaign
     */
    public Optional<ThinningPlanData> findThinningPlan(PlotId plotId, int campaignYear) {
        return thinningContextFacade.findThinningPlanDetail(plotId.plotId(), campaignYear)
                .map(detail -> new ThinningPlanData(detail.issued(), detail.percentageToRemove(),
                        detail.preThinningFruitsPerShoot(), detail.targetFruitsPerShoot(), detail.windowOpensOn(),
                        detail.windowClosesOn(), detail.sampledTrees(), detail.lastSampledOn(), detail.executedOn(),
                        detail.caliberProjectionStatus(), detail.mostLikelyFruitsPerKg(),
                        detail.commercialSizeGrade(), detail.estimatedKgPerHaAfterThinning()));
    }

    /**
     * Resolves the outcome of the confirmed thinning of a plot campaign.
     *
     * @param plotId       the assistant PlotId value object
     * @param campaignYear the campaign year
     * @return the outcome, or empty before the thinning is confirmed (or without its day)
     */
    public Optional<ThinningOutcomeData> findThinningOutcome(PlotId plotId, int campaignYear) {
        return thinningContextFacade.findThinningOutcome(plotId.plotId(), campaignYear)
                .filter(outcome -> outcome.executedOn() != null)
                .map(outcome -> new ThinningOutcomeData(outcome.executedOn(), outcome.actualRemovalPercentage(),
                        outcome.timeliness(), outcome.preThinningFruitsPerShoot(), outcome.residualFruitsPerShoot(),
                        outcome.targetFruitsPerShoot(), outcome.loadState(), outcome.kgPerHaWithoutThinning(),
                        outcome.kgPerHaAfterThinning(), outcome.yieldBasis(), outcome.harvestCampaigns(),
                        outcome.profileStatus(), outcome.windowOpensDaysAfterBloom(),
                        outcome.windowClosesDaysAfterBloom()));
    }
}

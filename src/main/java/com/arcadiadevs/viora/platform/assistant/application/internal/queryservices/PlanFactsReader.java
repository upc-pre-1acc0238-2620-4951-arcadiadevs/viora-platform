package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlanFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.CampaignThinningPlan;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import org.springframework.stereotype.Service;

/**
 * Reads the figures of the thinning plan screen: the load, target and window of the plan of the campaign, and how far
 * its prescription and sampling got.
 */
@Service
public class PlanFactsReader implements ScreenFactsReader {

    private final ExternalThinningService externalThinningService;

    /**
     * Constructs the reader.
     *
     * @param externalThinningService reads the plan and the prescription of the campaign
     */
    public PlanFactsReader(ExternalThinningService externalThinningService) {
        this.externalThinningService = externalThinningService;
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.PLAN;
    }

    @Override
    public ScreenFacts read(PlotId plotId, int campaignYear) {
        var plan = externalThinningService.findCampaignThinningPlan(plotId, campaignYear);
        var availability = externalThinningService.findPrescriptionAvailability(plotId, campaignYear);
        return new PlanFacts(
                campaignYear,
                plan.map(CampaignThinningPlan::preThinningFruitsPerShoot).orElse(null),
                plan.map(CampaignThinningPlan::targetFruitsPerShoot).orElse(null),
                plan.map(CampaignThinningPlan::percentageToRemove).orElse(null),
                plan.map(CampaignThinningPlan::windowOpensOn).orElse(null),
                plan.map(CampaignThinningPlan::windowClosesOn).orElse(null),
                availability.map(PrescriptionAvailability::sampledTrees).orElse(0),
                availability.map(PrescriptionAvailability::issued).orElse(false),
                availability.map(PrescriptionAvailability::percentageToRemove).orElse(null),
                availability.map(PrescriptionAvailability::treesNeeded).orElse(0));
    }
}

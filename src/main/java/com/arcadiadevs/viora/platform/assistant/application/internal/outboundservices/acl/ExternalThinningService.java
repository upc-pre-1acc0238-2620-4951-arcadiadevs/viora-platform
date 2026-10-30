package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Outbound ACL service used by the assistant to read how far the thinning prescription of a plot got.
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
     * Resolves the thinning plan of a plot campaign with the figures the assistant explains.
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
}

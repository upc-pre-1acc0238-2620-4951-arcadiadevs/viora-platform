package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.CampaignThinningPlan;
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
     * Resolves the crop load, target and window of the prescription of a plot campaign.
     *
     * @param plotId       the assistant PlotId value object
     * @param campaignYear the campaign year
     * @return the plan, or empty when the plot was never sampled that campaign
     */
    public Optional<CampaignThinningPlan> findCampaignThinningPlan(PlotId plotId, int campaignYear) {
        return thinningContextFacade.findCampaignThinningPlan(plotId.plotId(), campaignYear);
    }
}

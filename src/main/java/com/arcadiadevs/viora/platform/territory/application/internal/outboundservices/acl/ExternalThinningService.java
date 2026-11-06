package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Anti-corruption layer reading from Thinning the sampling, load and prescription of a plot's campaign.
 */
@Service("territoryExternalThinningService")
public class ExternalThinningService {

    private final ThinningContextFacade thinningContextFacade;

    /**
     * Creates the service.
     *
     * @param thinningContextFacade the Thinning facade
     */
    public ExternalThinningService(ThinningContextFacade thinningContextFacade) {
        this.thinningContextFacade = thinningContextFacade;
    }

    /**
     * Finds what Thinning knows of a plot's campaign.
     *
     * @param plotId       the plot
     * @param campaignYear the campaign
     * @return the facts, or empty if the plot has no prescription this campaign
     */
    public Optional<ThinningFacts> findCampaign(PlotId plotId, int campaignYear) {
        return thinningContextFacade.findThinningPlanDetail(plotId.plotId(), campaignYear)
                .map(detail -> new ThinningFacts(detail.issued(), detail.percentageToRemove(),
                        detail.preThinningFruitsPerShoot(), detail.targetFruitsPerShoot(), detail.windowOpensOn(),
                        detail.windowClosesOn(), detail.sampledTrees(), detail.lastSampledOn(), detail.executedOn(),
                        detail.estimatedKgPerHaAfterThinning()));
    }
}

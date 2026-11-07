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

    /**
     * Finds how much a plot is expected to yield in a campaign.
     *
     * @param plotId       the plot
     * @param campaignYear the campaign
     * @return the outlook, or empty if the plot has no prescription that campaign
     */
    public Optional<YieldOutlookFacts> findYieldOutlook(PlotId plotId, int campaignYear) {
        return thinningContextFacade.findYieldOutlook(plotId.plotId(), campaignYear)
                .map(outlook -> new YieldOutlookFacts(outlook.sampled(), outlook.kgPerHaWithLoad(), outlook.thinned(),
                        outlook.kgPerHaAfterThinning()));
    }

    /**
     * How much a plot is expected to yield, per hectare.
     *
     * @param sampled              whether any tree was sampled
     * @param kgPerHaWithLoad      the yield with the sampled load; null unless it can be estimated
     * @param thinned              whether the thinning was registered
     * @param kgPerHaAfterThinning the yield with the load the thinning left; null unless thinned and estimable
     */
    public record YieldOutlookFacts(boolean sampled, Double kgPerHaWithLoad, boolean thinned,
                                    Double kgPerHaAfterThinning) {

        /**
         * The yield the intake adds: after the thinning only once it was registered, never assuming one that did not
         * happen.
         *
         * @return the kilograms per hectare, or null when the plot cannot be estimated
         */
        public Double expectedKgPerHa() {
            return thinned && kgPerHaAfterThinning != null ? kgPerHaAfterThinning : kgPerHaWithLoad;
        }
    }
}

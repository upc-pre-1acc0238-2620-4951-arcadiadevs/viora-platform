package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Outbound ACL service used by the assistant to read the harvest history and alternate bearing of a plot.
 */
@Service("assistantExternalPhenologyService")
public class ExternalPhenologyService {

    private final PhenologyContextFacade phenologyContextFacade;

    public ExternalPhenologyService(PhenologyContextFacade phenologyContextFacade) {
        this.phenologyContextFacade = phenologyContextFacade;
    }

    /**
     * Resolves the latest Biennial Bearing Index of the plot.
     *
     * @param plotId the assistant PlotId value object
     * @return the index, or empty while there are too few campaigns to calculate it
     */
    public Optional<Double> findBiennialBearingIndex(PlotId plotId) {
        return phenologyContextFacade.findBiennialBearingIndex(plotId.plotId())
                .map(PhenologyContextFacade.PlotBearingIndex::value);
    }

    /**
     * Counts the campaigns with a registered harvest.
     *
     * @param plotId the assistant PlotId value object
     * @return number of historical campaigns
     */
    public int countHistoricalCampaigns(PlotId plotId) {
        return phenologyContextFacade.findHistoricalYields(plotId.plotId()).size();
    }
}

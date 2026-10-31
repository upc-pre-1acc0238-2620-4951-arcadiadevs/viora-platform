package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CampaignClosedFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotBearingIndex;
import org.springframework.stereotype.Service;

/**
 * Reads the figures of the closed campaign screen: the harvests and bearing index of the plot, its area and the
 * outcome of the confirmed thinning of the closed campaign (the latest one with a recorded harvest). Without a
 * harvest nothing else is read.
 */
@Service
public class CampaignClosedFactsReader implements ScreenFactsReader {

    private final ExternalPhenologyService externalPhenologyService;
    private final ExternalThinningService externalThinningService;
    private final ExternalOrchardService externalOrchardService;

    /**
     * Constructs the reader.
     *
     * @param externalPhenologyService reads the harvests and the bearing index
     * @param externalThinningService  reads the thinning of the closed campaign
     * @param externalOrchardService   reads the area of the plot
     */
    public CampaignClosedFactsReader(ExternalPhenologyService externalPhenologyService,
                                     ExternalThinningService externalThinningService,
                                     ExternalOrchardService externalOrchardService) {
        this.externalPhenologyService = externalPhenologyService;
        this.externalThinningService = externalThinningService;
        this.externalOrchardService = externalOrchardService;
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.CAMPAIGN_CLOSED;
    }

    @Override
    public ScreenFacts read(PlotId plotId, int campaignYear) {
        var yields = externalPhenologyService.findHistoricalYields(plotId);
        if (yields.isEmpty()) {
            return new CampaignClosedFacts(yields, null, null, null, null);
        }
        var bearing = externalPhenologyService.findBearing(plotId);
        return new CampaignClosedFacts(
                yields,
                bearing.map(PlotBearingIndex::value).orElse(null),
                bearing.map(PlotBearingIndex::category).orElse(null),
                externalThinningService.findThinningOutcome(plotId, yields.lastKey()).orElse(null),
                externalOrchardService.findAreaHectares(plotId).orElse(null));
    }
}

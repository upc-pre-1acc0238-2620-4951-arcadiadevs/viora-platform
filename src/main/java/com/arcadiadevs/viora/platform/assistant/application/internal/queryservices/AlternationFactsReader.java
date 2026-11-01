package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AlternationFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotBearingIndex;
import org.springframework.stereotype.Service;

/**
 * Reads the figures of the alternation screen: the area of the plot, its harvest history and bearing index, and
 * whether the caliber of the campaign is estimated.
 */
@Service
public class AlternationFactsReader implements ScreenFactsReader {

    private static final String CALIBER_ESTIMATED = "ESTIMATED";

    private final ExternalOrchardService externalOrchardService;
    private final ExternalPhenologyService externalPhenologyService;
    private final ExternalThinningService externalThinningService;

    /**
     * Constructs the reader.
     *
     * @param externalOrchardService   reads the area of the plot
     * @param externalPhenologyService reads the harvest history and the bearing index
     * @param externalThinningService  reads the caliber projection of the campaign
     */
    public AlternationFactsReader(ExternalOrchardService externalOrchardService,
                                  ExternalPhenologyService externalPhenologyService,
                                  ExternalThinningService externalThinningService) {
        this.externalOrchardService = externalOrchardService;
        this.externalPhenologyService = externalPhenologyService;
        this.externalThinningService = externalThinningService;
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.ALTERNATION;
    }

    @Override
    public ScreenFacts read(PlotId plotId, int campaignYear) {
        var bearing = externalPhenologyService.findBearing(plotId);
        var prescription = externalThinningService.findPrescriptionAvailability(plotId, campaignYear);
        return new AlternationFacts(
                campaignYear,
                externalOrchardService.findAreaHectares(plotId).orElse(null),
                externalPhenologyService.findHistoricalYields(plotId),
                bearing.map(PlotBearingIndex::value).orElse(null),
                bearing.map(PlotBearingIndex::category).orElse(null),
                prescription.map(p -> CALIBER_ESTIMATED.equals(p.caliberProjectionStatus())).orElse(false));
    }
}

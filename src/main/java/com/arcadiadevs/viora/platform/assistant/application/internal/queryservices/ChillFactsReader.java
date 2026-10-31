package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.springframework.stereotype.Service;

/**
 * Reads the figures of the winter chill screen: the season the chill metric of Phenology describes today and the one
 * before it, with the same Erez model, weather and varietal requirement. The assistant never recalculates portions.
 */
@Service
public class ChillFactsReader implements ScreenFactsReader {

    private final ExternalPhenologyService externalPhenologyService;

    /**
     * Constructs the reader.
     *
     * @param externalPhenologyService reads the chill seasons of the plot
     */
    public ChillFactsReader(ExternalPhenologyService externalPhenologyService) {
        this.externalPhenologyService = externalPhenologyService;
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.WINTER_CHILL;
    }

    @Override
    public ScreenFacts read(PlotId plotId, int campaignYear) {
        return externalPhenologyService.findChillSeasons(plotId);
    }
}

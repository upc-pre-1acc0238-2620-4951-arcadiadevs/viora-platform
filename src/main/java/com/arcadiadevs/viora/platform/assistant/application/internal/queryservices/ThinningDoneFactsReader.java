package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningDoneFacts;
import org.springframework.stereotype.Service;

/**
 * Reads the figures of the screen shown after the thinning is recorded: the outcome of the confirmed thinning of the
 * campaign, as Thinning computes it.
 */
@Service
public class ThinningDoneFactsReader implements ScreenFactsReader {

    private final ExternalThinningService externalThinningService;

    /**
     * Constructs the reader.
     *
     * @param externalThinningService reads the outcome of the confirmed thinning
     */
    public ThinningDoneFactsReader(ExternalThinningService externalThinningService) {
        this.externalThinningService = externalThinningService;
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.THINNING_DONE;
    }

    @Override
    public ScreenFacts read(PlotId plotId, int campaignYear) {
        return new ThinningDoneFacts(campaignYear,
                externalThinningService.findThinningOutcome(plotId, campaignYear).orElse(null));
    }
}

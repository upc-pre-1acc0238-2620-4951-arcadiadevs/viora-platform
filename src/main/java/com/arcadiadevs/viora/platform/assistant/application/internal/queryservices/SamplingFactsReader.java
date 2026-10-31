package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.SamplingFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Reads the figures of the sampling summary screen: the sampling and the thinning plan of the campaign, and the trees
 * still missing for a representative sampling.
 */
@Service
public class SamplingFactsReader implements ScreenFactsReader {

    private final ExternalThinningService externalThinningService;
    private final Clock clock;

    /**
     * Constructs the reader.
     *
     * @param externalThinningService reads the plan and the sampling of the campaign
     * @param clock                   the application clock, for the day the window is judged against
     */
    public SamplingFactsReader(ExternalThinningService externalThinningService, Clock clock) {
        this.externalThinningService = externalThinningService;
        this.clock = clock;
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.SAMPLING_SUMMARY;
    }

    @Override
    public ScreenFacts read(PlotId plotId, int campaignYear) {
        return new SamplingFacts(
                LocalDate.now(clock.withZone(ProducerTimeZone.ZONE)),
                externalThinningService.findThinningPlan(plotId, campaignYear).orElse(null),
                externalThinningService.findPrescriptionAvailability(plotId, campaignYear)
                        .map(PrescriptionAvailability::treesNeeded).orElse(0));
    }
}

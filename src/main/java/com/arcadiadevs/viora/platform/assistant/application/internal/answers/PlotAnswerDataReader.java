package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotBearingIndex;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;

/**
 * Reads, through the ACL services, the data of a plot the facts assemblers of a screen answer from.
 */
@Component
public class PlotAnswerDataReader {

    private final ExternalPhenologyService externalPhenologyService;
    private final ExternalThinningService externalThinningService;
    private final Clock clock;

    public PlotAnswerDataReader(ExternalPhenologyService externalPhenologyService,
            ExternalThinningService externalThinningService, Clock clock) {
        this.externalPhenologyService = externalPhenologyService;
        this.externalThinningService = externalThinningService;
        this.clock = clock;
    }

    /**
     * Reads the data of a plot for the questions of a screen.
     *
     * @param screen the screen
     * @param plotId the plot
     * @return the data; the campaign is the calendar year in the producers' time zone, as in the catalog
     */
    public PlotAnswerData read(ScreenKey screen, PlotId plotId) {
        var today = LocalDate.now(clock.withZone(ProducerTimeZone.ZONE));
        int campaignYear = today.getYear();
        var plan = externalThinningService.findThinningPlan(plotId, campaignYear).orElse(null);
        if (screen != ScreenKey.ALTERNATION) {
            return new PlotAnswerData(plotId, today, campaignYear, null, null, Collections.emptySortedMap(), plan);
        }
        var index = externalPhenologyService.findBearingIndexWithBand(plotId);
        return new PlotAnswerData(plotId, today, campaignYear,
                index.map(PlotBearingIndex::value).orElse(null),
                index.map(PlotBearingIndex::category).orElse(null),
                externalPhenologyService.findHistoricalYields(plotId),
                plan);
    }
}

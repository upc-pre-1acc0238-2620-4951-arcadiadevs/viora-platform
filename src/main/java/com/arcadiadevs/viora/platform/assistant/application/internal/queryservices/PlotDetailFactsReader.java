package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotDetailFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotBearingIndex;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Reads the figures of the plot detail screen: the thinning plan and sampling of the campaign, the bearing index and
 * harvests, the open alerts with the thresholds of Telemetry and the forecast, as the home screen reads one plot.
 */
@Service
public class PlotDetailFactsReader implements ScreenFactsReader {

    private final ExternalThinningService externalThinningService;
    private final ExternalPhenologyService externalPhenologyService;
    private final ExternalTelemetryService externalTelemetryService;
    private final Clock clock;

    /**
     * Constructs the reader.
     *
     * @param externalThinningService  reads the plan and the sampling of the campaign
     * @param externalPhenologyService reads the bearing index and the harvests
     * @param externalTelemetryService reads the open alerts, the thresholds and the forecast
     * @param clock                    the application clock, for the day the window is judged against
     */
    public PlotDetailFactsReader(ExternalThinningService externalThinningService,
                                 ExternalPhenologyService externalPhenologyService,
                                 ExternalTelemetryService externalTelemetryService, Clock clock) {
        this.externalThinningService = externalThinningService;
        this.externalPhenologyService = externalPhenologyService;
        this.externalTelemetryService = externalTelemetryService;
        this.clock = clock;
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.PLOT_DETAIL;
    }

    @Override
    public ScreenFacts read(PlotId plotId, int campaignYear) {
        var bearing = externalPhenologyService.findBearing(plotId);
        return new PlotDetailFacts(
                LocalDate.now(clock.withZone(ProducerTimeZone.ZONE)),
                campaignYear,
                externalThinningService.findThinningPlan(plotId, campaignYear).orElse(null),
                externalThinningService.findPrescriptionAvailability(plotId, campaignYear)
                        .map(PrescriptionAvailability::treesNeeded).orElse(0),
                bearing.map(PlotBearingIndex::value).orElse(null),
                bearing.map(PlotBearingIndex::category).orElse(null),
                externalPhenologyService.findHistoricalYields(plotId).size(),
                externalTelemetryService.findOpenIncidents(plotId),
                externalTelemetryService.findThresholds(),
                externalTelemetryService.findForecast(plotId).orElse(null));
    }
}

package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotBearingIndex;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Reads the figures of the home screen through the Thinning, Phenology and Telemetry facades: of each plot, its
 * thinning plan and the trees its sampling still misses, its harvests and bearing index, its open alerts and its
 * forecast.
 */
@Service
public class HomeFactsReader implements ScreenFactsReader {

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
     * @param clock                    the application clock, for the day the week is judged from
     */
    public HomeFactsReader(ExternalThinningService externalThinningService,
                           ExternalPhenologyService externalPhenologyService,
                           ExternalTelemetryService externalTelemetryService, Clock clock) {
        this.externalThinningService = externalThinningService;
        this.externalPhenologyService = externalPhenologyService;
        this.externalTelemetryService = externalTelemetryService;
        this.clock = clock;
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.HOME;
    }

    @Override
    public ScreenFacts read(PlotId plotId, int campaignYear) {
        return facts(false, List.of(plotId), campaignYear);
    }

    private HomeFacts facts(boolean fieldScope, List<PlotId> plotIds, int campaignYear) {
        var plots = plotIds.stream().map(plotId -> plot(plotId, campaignYear)).toList();
        return new HomeFacts(LocalDate.now(clock.withZone(ProducerTimeZone.ZONE)), campaignYear, fieldScope, plots,
                externalTelemetryService.findThresholds());
    }

    private HomeFacts.PlotHome plot(PlotId plotId, int campaignYear) {
        var bearing = externalPhenologyService.findBearing(plotId);
        return new HomeFacts.PlotHome(
                externalThinningService.findThinningPlan(plotId, campaignYear).orElse(null),
                externalThinningService.findPrescriptionAvailability(plotId, campaignYear)
                        .map(PrescriptionAvailability::treesNeeded).orElse(0),
                externalTelemetryService.findOpenIncidents(plotId),
                externalPhenologyService.findHistoricalYields(plotId),
                bearing.map(PlotBearingIndex::value).orElse(null),
                bearing.map(PlotBearingIndex::category).orElse(null),
                externalTelemetryService.findForecast(plotId).orElse(null));
    }
}

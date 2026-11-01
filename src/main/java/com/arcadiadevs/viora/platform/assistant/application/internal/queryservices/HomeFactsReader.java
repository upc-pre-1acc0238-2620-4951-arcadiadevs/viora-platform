package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
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
 * Reads the figures of the home screen through the Orchard, Thinning, Phenology and Telemetry facades: of the plot
 * asked about, or of every active plot of the producer (its whole field). Of each plot, its thinning plan and the
 * trees its sampling still misses, its harvests and bearing index, its open alerts and its forecast.
 */
@Service
public class HomeFactsReader implements ScreenFactsReader {

    private final ExternalOrchardService externalOrchardService;
    private final ExternalThinningService externalThinningService;
    private final ExternalPhenologyService externalPhenologyService;
    private final ExternalTelemetryService externalTelemetryService;
    private final Clock clock;

    /**
     * Constructs the reader.
     *
     * @param externalOrchardService   lists the active plots of the producer, for the whole field
     * @param externalThinningService  reads the plan and the sampling of the campaign
     * @param externalPhenologyService reads the bearing index and the harvests
     * @param externalTelemetryService reads the open alerts, the thresholds and the forecast
     * @param clock                    the application clock, for the day the week is judged from
     */
    public HomeFactsReader(ExternalOrchardService externalOrchardService,
                           ExternalThinningService externalThinningService,
                           ExternalPhenologyService externalPhenologyService,
                           ExternalTelemetryService externalTelemetryService, Clock clock) {
        this.externalOrchardService = externalOrchardService;
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
        if (plotId == null) {
            throw new IllegalArgumentException("assistant.plot.required");
        }
        return facts(false, List.of(plotId), campaignYear);
    }

    /**
     * Reads the figures of one plot or, without plot, of every active plot of the producer.
     *
     * @param producerId   the producer asking; only its own plots are read
     * @param plotId       the plot; null for the whole field
     * @param campaignYear the current campaign year
     * @return the figures of the plot or of the field
     */
    @Override
    public ScreenFacts read(String producerId, PlotId plotId, int campaignYear) {
        if (plotId != null) {
            return read(plotId, campaignYear);
        }
        return facts(true, externalOrchardService.findActivePlotIdsOfProducer(producerId), campaignYear);
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

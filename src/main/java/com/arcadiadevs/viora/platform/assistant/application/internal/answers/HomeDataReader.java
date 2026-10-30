package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotBearingIndex;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reads, through the ACL services, what the HOME questions need of one plot or, without plot, of every active plot
 * of the asking producer. The catalog only reads the harvests and the thinning plans; the answers also read the
 * alerts, the bearing index and the forecast.
 */
@Component
public class HomeDataReader {

    private final ExternalOrchardService externalOrchardService;
    private final ExternalThinningService externalThinningService;
    private final ExternalPhenologyService externalPhenologyService;
    private final ExternalTelemetryService externalTelemetryService;

    public HomeDataReader(ExternalOrchardService externalOrchardService,
            ExternalThinningService externalThinningService, ExternalPhenologyService externalPhenologyService,
            ExternalTelemetryService externalTelemetryService) {
        this.externalOrchardService = externalOrchardService;
        this.externalThinningService = externalThinningService;
        this.externalPhenologyService = externalPhenologyService;
        this.externalTelemetryService = externalTelemetryService;
    }

    /**
     * Reads what the catalog needs to decide which HOME questions can be answered.
     *
     * @param producerId   the producer asking; its plots are the field
     * @param plotId       the plot; null for the whole field
     * @param campaignYear current campaign year
     * @return the plans and harvests of the plot or of every active plot of the producer
     */
    public HomeData readForCatalog(String producerId, PlotId plotId, int campaignYear) {
        var plots = plotsOf(producerId, plotId).stream()
                .map(plot -> new HomeData.PlotHome(
                        externalThinningService.findThinningPlan(plot, campaignYear).orElse(null), 0, List.of(),
                        externalPhenologyService.findHistoricalYields(plot), null, null, null))
                .toList();
        return new HomeData(plotId == null, plots, null);
    }

    /**
     * Reads everything the HOME answers use.
     *
     * @param producerId   the producer asking; its plots are the field
     * @param plotId       the plot; null for the whole field
     * @param campaignYear current campaign year
     * @return the data of the plot or of every active plot of the producer, with the alert thresholds
     */
    public HomeData readForAnswer(String producerId, PlotId plotId, int campaignYear) {
        var plots = plotsOf(producerId, plotId).stream()
                .map(plot -> {
                    var index = externalPhenologyService.findBearingIndexWithBand(plot);
                    return new HomeData.PlotHome(
                            externalThinningService.findThinningPlan(plot, campaignYear).orElse(null),
                            externalThinningService.findPrescriptionAvailability(plot, campaignYear)
                                    .map(PrescriptionAvailability::treesNeeded).orElse(0),
                            externalTelemetryService.findOpenIncidents(plot),
                            externalPhenologyService.findHistoricalYields(plot),
                            index.map(PlotBearingIndex::value).orElse(null),
                            index.map(PlotBearingIndex::category).orElse(null),
                            externalTelemetryService.findForecast(plot).orElse(null));
                })
                .toList();
        return new HomeData(plotId == null, plots, externalTelemetryService.findThresholds());
    }

    /** The plot asked about, or the active plots of the producer (only its own: plot access is per producer). */
    private List<PlotId> plotsOf(String producerId, PlotId plotId) {
        return plotId != null ? List.of(plotId) : externalOrchardService.findActivePlotIdsOfProducer(producerId);
    }
}

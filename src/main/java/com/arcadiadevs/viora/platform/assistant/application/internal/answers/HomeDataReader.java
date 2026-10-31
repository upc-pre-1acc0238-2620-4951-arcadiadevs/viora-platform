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

    /**
     * Adds what CAMPAIGN_CLOSED reads of one plot besides the HOME data: the surface of the plot and the outcome of
     * the confirmed thinning of the closed campaign (the latest one with a recorded harvest).
     *
     * @param plot   the plot, as {@link #readForAnswer} reads one plot
     * @param plotId the plot
     * @return the same data with the surface and, when the closed campaign was thinned, its outcome
     */
    public HomeData withClosedCampaign(HomeData plot, PlotId plotId) {
        var outcome = plot.closedCampaignYear()
                .flatMap(closedYear -> externalThinningService.findThinningOutcome(plotId, closedYear))
                .orElse(null);
        return plot.withOutcome(outcome).withArea(externalOrchardService.findPlotAreaHectares(plotId).orElse(null));
    }

    /** The plot asked about, or the active plots of the producer (only its own: plot access is per producer). */
    private List<PlotId> plotsOf(String producerId, PlotId plotId) {
        return plotId != null ? List.of(plotId) : externalOrchardService.findActivePlotIdsOfProducer(producerId);
    }
}

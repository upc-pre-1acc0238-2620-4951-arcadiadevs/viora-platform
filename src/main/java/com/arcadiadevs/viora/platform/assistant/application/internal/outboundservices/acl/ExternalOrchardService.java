package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Outbound ACL service used by the assistant to verify the plot it is asked about, list its owner's plots and find the
 * producers the daily summary job prepares.
 */
@Service("assistantExternalOrchardService")
public class ExternalOrchardService {

    private final OrchardContextFacade orchardContextFacade;

    public ExternalOrchardService(OrchardContextFacade orchardContextFacade) {
        this.orchardContextFacade = orchardContextFacade;
    }

    /**
     * Checks if the plot exists and is active.
     *
     * @param plotId the assistant PlotId value object
     * @return true if the plot exists and is active
     */
    public boolean existsActivePlot(PlotId plotId) {
        return plotId != null && orchardContextFacade.existsActivePlot(plotId.plotId());
    }

    /**
     * Resolves the area of the plot.
     *
     * @param plotId the assistant PlotId value object
     * @return the area in hectares, or empty when the plot is unknown
     */
    public Optional<Double> findAreaHectares(PlotId plotId) {
        return orchardContextFacade.findPlotAreaHectares(plotId.plotId());
    }

    /**
     * Lists the active plots of the producer who owns a plot. Plot access already requires the asking producer to
     * own the plot, so these are the asking producer's plots and never another producer's.
     *
     * @param plotId the assistant PlotId value object
     * @return the active plot identifiers of its owner; empty when the plot is unknown
     */
    public List<String> findActivePlotIdsOfOwner(PlotId plotId) {
        return orchardContextFacade.findPlotProducerId(plotId.plotId())
                .map(orchardContextFacade::findActivePlotIdsByProducerId)
                .orElse(List.of());
    }

    /**
     * Lists the active plots of a producer, for the questions about its whole field. The producer is always the
     * authenticated one, so no other producer's plot is ever read.
     *
     * @param producerId the producer asking
     * @return its active plots, in the order the Orchard context lists them
     */
    public List<PlotId> findActivePlotIdsOfProducer(String producerId) {
        return orchardContextFacade.findActivePlotIdsByProducerId(producerId).stream().map(PlotId::new).toList();
    }

    /**
     * Lists the producers that own at least one active plot, for the daily summary job. It reads every active plot
     * and its owner, one lookup per plot; a plot without a producer is skipped.
     *
     * @return the producers, each once, sorted
     */
    public List<String> findProducersWithActivePlots() {
        return orchardContextFacade.findAllActivePlotIds().stream()
                .map(orchardContextFacade::findPlotProducerId)
                .flatMap(Optional::stream)
                .distinct()
                .sorted()
                .toList();
    }
}

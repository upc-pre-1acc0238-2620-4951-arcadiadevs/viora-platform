package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Outbound ACL service used by the assistant to verify the plot it is asked about and list its owner's plots.
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
}

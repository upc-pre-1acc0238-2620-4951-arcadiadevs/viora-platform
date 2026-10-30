package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Outbound ACL service used by the assistant to verify the plot it is asked about.
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
}

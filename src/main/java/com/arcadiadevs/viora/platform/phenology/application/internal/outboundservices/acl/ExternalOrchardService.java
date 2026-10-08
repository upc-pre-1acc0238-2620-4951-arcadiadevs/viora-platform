package com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Outbound ACL service used by the Phenology bounded context to interact with Orchard capabilities.
 *
 * <p>Translates Phenology value objects to primitives expected by {@link OrchardContextFacade},
 * ensuring strict DDD decoupling.</p>
 */
@Service("phenologyExternalOrchardService")
public class ExternalOrchardService {

    private final OrchardContextFacade orchardContextFacade;

    /**
     * Constructs the outbound service injecting the Orchard ACL facade.
     *
     * @param orchardContextFacade the Orchard context facade
     */
    public ExternalOrchardService(OrchardContextFacade orchardContextFacade) {
        this.orchardContextFacade = orchardContextFacade;
    }

    /**
     * Checks if the referenced plot exists and is active in Orchard.
     *
     * @param plotId the Phenology PlotId value object
     * @return true if plot exists and is active; false otherwise
     */
    public boolean existsActivePlot(PlotId plotId) {
        if (plotId == null) {
            return false;
        }
        return orchardContextFacade.existsActivePlot(plotId.plotId());
    }

    /**
     * Retrieves the centroid of an active plot, where its weather is read.
     *
     * @param plotId the Phenology PlotId value object
     * @return Optional containing [latitude, longitude], or empty if the plot is not found
     */
    public Optional<double[]> findPlotCentroid(PlotId plotId) {
        if (plotId == null) {
            return Optional.empty();
        }
        return orchardContextFacade.findPlotCentroid(plotId.plotId());
    }
}

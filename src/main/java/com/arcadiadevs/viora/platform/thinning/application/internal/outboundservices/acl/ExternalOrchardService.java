package com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import org.springframework.stereotype.Service;

/**
 * Outbound ACL service used by Thinning context to interact with Orchard capabilities.
 *
 * <p>Ensures that plot existence, active status, and cadastral references are verified
 * across context boundaries without importing internal Orchard domain entities.</p>
 */
@Service("thinningExternalOrchardService")
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
     * Checks if the referenced plot exists and is currently in active operational status.
     *
     * @param plotId the Thinning PlotId value object
     * @return true if plot exists and is active; false otherwise
     */
    public boolean existsActivePlot(PlotId plotId) {
        if (plotId == null) {
            return false;
        }
        return orchardContextFacade.existsActivePlot(plotId.plotId());
    }
}

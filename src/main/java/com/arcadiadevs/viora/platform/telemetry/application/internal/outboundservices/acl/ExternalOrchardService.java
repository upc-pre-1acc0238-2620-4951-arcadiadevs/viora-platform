package com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import org.springframework.stereotype.Service;

/**
 * Outbound ACL service used by the Telemetry bounded context to interact with Orchard capabilities.
 *
 * <p>Translates Telemetry value objects to primitives expected by {@link OrchardContextFacade},
 * ensuring strict DDD decoupling.</p>
 */
@Service("telemetryExternalOrchardService")
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
     * @param plotId the Telemetry PlotId value object
     * @return true if plot exists and is active; false otherwise
     */
    public boolean existsActivePlot(PlotId plotId) {
        if (plotId == null) {
            return false;
        }
        return orchardContextFacade.existsActivePlot(plotId.plotId());
    }
}

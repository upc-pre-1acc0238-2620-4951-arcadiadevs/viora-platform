package com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    /**
     * Retrieves the calculated centroid coordinates [latitude, longitude] for an active plot.
     *
     * @param plotId the Telemetry PlotId value object
     * @return Optional containing double array [latitude, longitude], or empty if plot is not found or not active
     */
    public Optional<double[]> findPlotCentroid(PlotId plotId) {
        if (plotId == null) {
            return Optional.empty();
        }
        return orchardContextFacade.findPlotCentroid(plotId.plotId());
    }

    /**
     * Retrieves the human-readable name of an active plot.
     *
     * @param plotId the Telemetry PlotId value object
     * @return Optional containing the plot name, or empty if not found
     */
    public Optional<String> findPlotName(PlotId plotId) {
        if (plotId == null) {
            return Optional.empty();
        }
        return orchardContextFacade.findPlotName(plotId.plotId());
    }

    /**
     * Retrieves the botanical olive variety of an active plot.
     *
     * @param plotId the Telemetry PlotId value object
     * @return Optional containing the variety name, or empty if not found
     */
    public Optional<String> findPlotVariety(PlotId plotId) {
        if (plotId == null) {
            return Optional.empty();
        }
        return orchardContextFacade.findPlotVariety(plotId.plotId());
    }

    /**
     * Retrieves the owner producer identifier of an active plot.
     *
     * @param plotId the Telemetry PlotId value object
     * @return Optional containing the producer identifier, or empty if not found
     */
    public Optional<String> findPlotProducerId(PlotId plotId) {
        if (plotId == null) {
            return Optional.empty();
        }
        return orchardContextFacade.findPlotProducerId(plotId.plotId());
    }
}

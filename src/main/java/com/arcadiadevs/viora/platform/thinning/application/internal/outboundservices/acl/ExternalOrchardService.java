package com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    /**
     * Resolves the olive variety of an active plot, used to pick the caliber model calibration.
     *
     * @param plotId the Thinning PlotId value object
     * @return the variety name, or empty when the plot is unknown or not active
     */
    public Optional<String> findPlotVariety(PlotId plotId) {
        if (plotId == null) {
            return Optional.empty();
        }
        return orchardContextFacade.findPlotVariety(plotId.plotId());
    }

    /**
     * Resolves the human-readable name of an active plot.
     *
     * @param plotId the Thinning PlotId value object
     * @return the plot name, or empty when the plot is unknown or not active
     */
    public Optional<String> findPlotName(PlotId plotId) {
        if (plotId == null) {
            return Optional.empty();
        }
        return orchardContextFacade.findPlotName(plotId.plotId());
    }

    /**
     * Resolves the surface area in hectares of an active plot.
     *
     * @param plotId the Thinning PlotId value object
     * @return the surface area in hectares, or empty when the plot is unknown or not active
     */
    public Optional<Double> findPlotAreaHectares(PlotId plotId) {
        if (plotId == null) {
            return Optional.empty();
        }
        return orchardContextFacade.findPlotAreaHectares(plotId.plotId());
    }

    /**
     * Retrieves all active plot identifiers associated with a given producer.
     *
     * @param producerId the producer identifier string (UUID)
     * @return list of Thinning PlotId value objects
     */
    public java.util.List<PlotId> findActivePlotIdsByProducerId(String producerId) {
        if (producerId == null || producerId.isBlank()) {
            return java.util.List.of();
        }
        return orchardContextFacade.findActivePlotIdsByProducerId(producerId).stream()
                .map(PlotId::new)
                .toList();
    }
}

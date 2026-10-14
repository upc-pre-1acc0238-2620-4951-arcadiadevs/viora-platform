package com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Outbound ACL used by Settlement to verify active plots, their owner producer and describe them. */
@Service("settlementExternalOrchardService")
public class ExternalOrchardService {

    private final OrchardContextFacade orchardContextFacade;

    public ExternalOrchardService(OrchardContextFacade orchardContextFacade) {
        this.orchardContextFacade = orchardContextFacade;
    }

    /**
     * Resolves the owner of an active plot.
     *
     * @param plotId plot to check
     * @return the owner producer, or empty when the plot does not exist or is not active
     */
    public Optional<UserId> findActivePlotOwner(PlotId plotId) {
        if (plotId == null) {
            return Optional.empty();
        }
        return orchardContextFacade.findPlotProducerId(plotId.plotId()).map(UserId::new);
    }

    /**
     * Describes a plot for the agronomic dossier.
     *
     * @param plotId plot to describe
     * @return its name, variety and area; each null when unknown
     */
    public PlotDescription describePlot(PlotId plotId) {
        if (plotId == null) {
            return new PlotDescription(null, null, null);
        }
        String id = plotId.plotId();
        return new PlotDescription(orchardContextFacade.findPlotName(id).orElse(null),
                orchardContextFacade.findPlotVariety(id).orElse(null),
                orchardContextFacade.findPlotAreaHectares(id).orElse(null));
    }

    /**
     * Name, variety and area of a plot.
     *
     * @param name         plot name
     * @param variety      olive variety
     * @param areaHectares plot area
     */
    public record PlotDescription(String name, String variety, Double areaHectares) {
    }
}

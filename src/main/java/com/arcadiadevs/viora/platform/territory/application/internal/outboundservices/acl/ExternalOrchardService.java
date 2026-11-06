package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.GeoPoint;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Anti-corruption layer reading from Orchard who owns a plot, whether it is active and its name.
 */
@Service("territoryExternalOrchardService")
public class ExternalOrchardService {

    private final OrchardContextFacade orchardContextFacade;

    /**
     * Creates the service.
     *
     * @param orchardContextFacade the Orchard facade
     */
    public ExternalOrchardService(OrchardContextFacade orchardContextFacade) {
        this.orchardContextFacade = orchardContextFacade;
    }

    /**
     * Finds the owner of an active plot.
     *
     * @param plotId the plot
     * @return the producer, or empty if the plot does not exist or is archived
     */
    public Optional<ProducerId> findActivePlotOwner(PlotId plotId) {
        return orchardContextFacade.findPlotProducerId(plotId.plotId()).map(ProducerId::new);
    }

    /**
     * Finds the name of a plot.
     *
     * @param plotId the plot
     * @return the name, or empty
     */
    public Optional<String> findPlotName(PlotId plotId) {
        return orchardContextFacade.findPlotName(plotId.plotId());
    }

    /**
     * Lists the active plots of a producer.
     *
     * @param producerId the producer
     * @return the plots
     */
    public List<PlotId> findActivePlots(ProducerId producerId) {
        return orchardContextFacade.findActivePlotIdsByProducerId(producerId.producerId()).stream()
                .map(PlotId::new)
                .toList();
    }

    /**
     * Finds what Orchard knows of an active plot.
     *
     * @param plotId the plot
     * @return the facts, or empty if the plot does not exist or is archived
     */
    public Optional<PlotFacts> findPlotFacts(PlotId plotId) {
        var id = plotId.plotId();
        return orchardContextFacade.findPlotGeoJson(id).map(geoJson -> new PlotFacts(plotId,
                orchardContextFacade.findPlotName(id).orElse(null),
                orchardContextFacade.findPlotVariety(id).orElse(null),
                orchardContextFacade.findPlotAreaHectares(id).orElse(null),
                orchardContextFacade.findPlotCentroid(id)
                        .map(center -> new GeoPoint(center[0], center[1]))
                        .orElse(null),
                geoJson));
    }
}

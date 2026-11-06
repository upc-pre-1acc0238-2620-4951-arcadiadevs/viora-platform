package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Anti-corruption layer reading from Phenology the biennial bearing index of a plot.
 */
@Service("territoryExternalPhenologyService")
public class ExternalPhenologyService {

    private final PhenologyContextFacade phenologyContextFacade;

    /**
     * Creates the service.
     *
     * @param phenologyContextFacade the Phenology facade
     */
    public ExternalPhenologyService(PhenologyContextFacade phenologyContextFacade) {
        this.phenologyContextFacade = phenologyContextFacade;
    }

    /**
     * Finds the biennial bearing index of a plot.
     *
     * @param plotId the plot
     * @return the index, or empty with too few harvests to compute it
     */
    public Optional<BearingIndexFacts> findBearingIndex(PlotId plotId) {
        return phenologyContextFacade.findBiennialBearingIndex(plotId.plotId())
                .map(index -> new BearingIndexFacts(index.value(), index.category()));
    }
}

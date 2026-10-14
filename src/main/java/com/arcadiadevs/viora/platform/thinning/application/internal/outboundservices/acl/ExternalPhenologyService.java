package com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.SortedMap;

/** Outbound ACL used by Thinning to read the harvest history of a plot (sustainable yield of US26). */
@Service("thinningExternalPhenologyService")
public class ExternalPhenologyService {

    private final PhenologyContextFacade phenologyContextFacade;

    public ExternalPhenologyService(PhenologyContextFacade phenologyContextFacade) {
        this.phenologyContextFacade = phenologyContextFacade;
    }

    /**
     * Returns the historical yields registered for a plot.
     *
     * @param plotId plot to read
     * @return total kilograms per campaign year, ascending; empty without history
     */
    public SortedMap<Integer, Double> findHistoricalYields(PlotId plotId) {
        if (plotId == null) {
            return Collections.emptySortedMap();
        }
        return phenologyContextFacade.findHistoricalYields(plotId.plotId());
    }
}

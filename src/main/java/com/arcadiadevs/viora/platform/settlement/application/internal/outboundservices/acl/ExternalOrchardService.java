package com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Outbound ACL used by Settlement to verify active plots and their owner producer. */
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
}

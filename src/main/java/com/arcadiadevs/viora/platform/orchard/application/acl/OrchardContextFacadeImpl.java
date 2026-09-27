package com.arcadiadevs.viora.platform.orchard.application.acl;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotStatus;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application-layer implementation of the {@link OrchardContextFacade} ACL interface.
 *
 * <p>Enforces context boundary isolation by wrapping internal Orchard domain repository queries
 * and exposing only primitive types or agnostic Published Language structures.</p>
 */
@Service
@Transactional(readOnly = true)
public class OrchardContextFacadeImpl implements OrchardContextFacade {

    private final PlotRepository plotRepository;

    /**
     * Constructs the facade implementation injecting the domain repository port.
     *
     * @param plotRepository domain repository port
     */
    public OrchardContextFacadeImpl(PlotRepository plotRepository) {
        this.plotRepository = plotRepository;
    }

    @Override
    public boolean existsActivePlot(String plotId) {
        if (plotId == null || plotId.isBlank()) {
            return false;
        }
        try {
            var domainPlotId = new PlotId(plotId);
            return plotRepository.findById(domainPlotId)
                    .filter(plot -> plot.snapshot().status() == PlotStatus.ACTIVE)
                    .isPresent();
        } catch (IllegalArgumentException ex) {
            // Malformed UUID or VO validation failure means plot does not exist
            return false;
        }
    }
}

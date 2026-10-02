package com.arcadiadevs.viora.platform.orchard.application.acl;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotStatus;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import com.arcadiadevs.viora.platform.orchard.domain.services.CadastralGeometryService;
import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
    private final CadastralGeometryService cadastralGeometryService;

    /**
     * Constructs the facade implementation injecting domain repository and default geometry service.
     *
     * @param plotRepository domain repository port
     */
    public OrchardContextFacadeImpl(PlotRepository plotRepository) {
        this(plotRepository, new CadastralGeometryService());
    }

    /**
     * Constructs the facade implementation injecting domain repository and geometry service.
     *
     * @param plotRepository           domain repository port
     * @param cadastralGeometryService cadastral geometry domain service
     */
    @org.springframework.beans.factory.annotation.Autowired
    public OrchardContextFacadeImpl(PlotRepository plotRepository, CadastralGeometryService cadastralGeometryService) {
        this.plotRepository = plotRepository;
        this.cadastralGeometryService = cadastralGeometryService != null ? cadastralGeometryService : new CadastralGeometryService();
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

    @Override
    public Optional<double[]> findPlotCentroid(String plotId) {
        if (plotId == null || plotId.isBlank()) {
            return Optional.empty();
        }
        try {
            var domainPlotId = new PlotId(plotId);
            return plotRepository.findById(domainPlotId)
                    .filter(plot -> plot.snapshot().status() == PlotStatus.ACTIVE)
                    .map(plot -> cadastralGeometryService.computeCentroid(plot.snapshot().geometry().geoJson()));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> findPlotVariety(String plotId) {
        if (plotId == null || plotId.isBlank()) {
            return Optional.empty();
        }
        try {
            var domainPlotId = new PlotId(plotId);
            return plotRepository.findById(domainPlotId)
                    .filter(plot -> plot.snapshot().status() == PlotStatus.ACTIVE)
                    .map(plot -> plot.snapshot().variety().name());
        } catch (IllegalArgumentException ex) {
            // Malformed UUID or VO validation failure means plot does not exist
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> findPlotProducerId(String plotId) {
        if (plotId == null || plotId.isBlank()) {
            return Optional.empty();
        }
        try {
            var domainPlotId = new PlotId(plotId);
            return plotRepository.findById(domainPlotId)
                    .filter(plot -> plot.snapshot().status() == PlotStatus.ACTIVE)
                    .map(plot -> plot.snapshot().producerId().producerId());
        } catch (IllegalArgumentException ex) {
            // Malformed UUID or VO validation failure means plot does not exist
            return Optional.empty();
        }
    }
}

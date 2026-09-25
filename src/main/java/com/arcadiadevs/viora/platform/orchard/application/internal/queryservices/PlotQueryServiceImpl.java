package com.arcadiadevs.viora.platform.orchard.application.internal.queryservices;

import com.arcadiadevs.viora.platform.orchard.application.queryservices.PlotQueryService;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetAllActivePlotsByProducerIdQuery;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link PlotQueryService} handling plot query use cases without conditional branching.
 */
@Service
@Transactional(readOnly = true)
public class PlotQueryServiceImpl implements PlotQueryService {

    private final PlotRepository plotRepository;

    /**
     * Constructs the query service with the required domain repository port.
     *
     * @param plotRepository the domain plot repository
     */
    public PlotQueryServiceImpl(PlotRepository plotRepository) {
        this.plotRepository = plotRepository;
    }

    @Override
    public List<Plot> handle(GetAllActivePlotsByProducerIdQuery query) {
        return plotRepository.findActiveByProducerId(query.producerId());
    }

    @Override
    public List<Plot> handle(GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery query) {
        return plotRepository.findByProducerIdAndUpdatedSince(query.producerId(), query.updatedSince());
    }
}

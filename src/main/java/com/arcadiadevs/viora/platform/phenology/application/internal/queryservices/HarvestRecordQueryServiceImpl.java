package com.arcadiadevs.viora.platform.phenology.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.application.queryservices.HarvestRecordQueryService;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.HistoricalHarvestEntrySnapshot;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetHarvestRecordsByPlotIdQuery;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service implementation for querying historical harvest records.
 */
@Service
@Transactional(readOnly = true)
public class HarvestRecordQueryServiceImpl implements HarvestRecordQueryService {

    private final ChillAccumulationTrackerRepository trackerRepository;
    private final ExternalOrchardService externalOrchardService;

    /**
     * Constructs the query service injecting dependencies.
     *
     * @param trackerRepository      the domain repository port
     * @param externalOrchardService the outbound ACL service for orchard verifications
     */
    public HarvestRecordQueryServiceImpl(
            ChillAccumulationTrackerRepository trackerRepository,
            ExternalOrchardService externalOrchardService
    ) {
        this.trackerRepository = trackerRepository;
        this.externalOrchardService = externalOrchardService;
    }

    @Override
    public Result<List<HistoricalHarvestEntrySnapshot>, ApplicationError> handle(GetHarvestRecordsByPlotIdQuery query) {
        var plotId = query.plotId();
        if (!externalOrchardService.existsActivePlot(plotId)) {
            return Result.failure(ApplicationError.notFound("Plot", plotId.plotId()));
        }

        var trackerOpt = trackerRepository.findByPlotId(plotId);
        if (trackerOpt.isEmpty()) {
            return Result.success(List.of());
        }

        var tracker = trackerOpt.get();
        var allEntries = tracker.snapshot().harvestHistory();

        if (query.campaignYear() == null) {
            return Result.success(allEntries);
        }

        var filtered = allEntries.stream()
                .filter(e -> e.campaignYear().value().equals(query.campaignYear().value()))
                .toList();

        return Result.success(filtered);
    }
}

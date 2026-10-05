package com.arcadiadevs.viora.platform.thinning.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetPlotSamplingStatesQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetPlotSamplingStatesQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotSamplingState;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import com.arcadiadevs.viora.platform.thinning.domain.services.SamplingCoverageEvaluator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application query service implementation retrieving sampling coverage states across all plots of a producer.
 */
@Service
@Transactional(readOnly = true)
public class GetPlotSamplingStatesQueryServiceImpl implements GetPlotSamplingStatesQueryService {

    private final FruitThinningPrescriptionRepository prescriptionRepository;
    private final ExternalOrchardService externalOrchardService;

    /**
     * Constructs the query service.
     *
     * @param prescriptionRepository repository port for thinning prescriptions
     * @param externalOrchardService outbound ACL service for orchard context queries
     */
    public GetPlotSamplingStatesQueryServiceImpl(
            FruitThinningPrescriptionRepository prescriptionRepository,
            ExternalOrchardService externalOrchardService
    ) {
        if (prescriptionRepository == null) {
            throw new IllegalArgumentException("thinning.repository.null");
        }
        if (externalOrchardService == null) {
            throw new IllegalArgumentException("thinning.orchard_service.null");
        }
        this.prescriptionRepository = prescriptionRepository;
        this.externalOrchardService = externalOrchardService;
    }

    @Override
    public Result<List<PlotSamplingState>, ApplicationError> handle(GetPlotSamplingStatesQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "thinning.query.null"));
        }

        List<PlotId> plotIds = externalOrchardService.findActivePlotIdsByProducerId(query.producerId());
        if (plotIds.isEmpty()) {
            return Result.success(List.of());
        }

        List<FruitThinningPrescription> prescriptions =
                prescriptionRepository.findByPlotIdInAndCampaignYear(plotIds, query.campaignYear());

        Map<String, FruitThinningPrescription> prescriptionByPlotId = prescriptions.stream()
                .collect(Collectors.toMap(p -> p.snapshot().plotId().plotId(), p -> p));

        List<PlotSamplingState> states = new ArrayList<>();
        for (PlotId plotId : plotIds) {
            String plotName = externalOrchardService.findPlotName(plotId).orElse("Plot " + plotId.plotId());
            String variety = externalOrchardService.findPlotVariety(plotId).orElse("UNKNOWN");
            Double areaHectares = externalOrchardService.findPlotAreaHectares(plotId).orElse(0.0);

            FruitThinningPrescription prescription = prescriptionByPlotId.get(plotId.plotId());
            final int sampledTreesCount;
            final int treesNeeded;
            final boolean isRepresentative;
            final String samplingStatus;

            if (prescription == null) {
                sampledTreesCount = 0;
                treesNeeded = SamplingCoverageEvaluator.MIN_REPRESENTATIVE_TREES;
                isRepresentative = false;
                samplingStatus = "NOT_STARTED";
            } else {
                var rounds = prescription.snapshot().samplingRounds();
                sampledTreesCount = SamplingCoverageEvaluator.countUniqueEvaluatedTreesFromSnapshots(rounds);
                isRepresentative = SamplingCoverageEvaluator.isRepresentativeFromSnapshots(rounds);
                treesNeeded = SamplingCoverageEvaluator.treesNeededFromSnapshots(rounds);
                if (sampledTreesCount == 0) {
                    samplingStatus = "NOT_STARTED";
                } else if (isRepresentative) {
                    samplingStatus = "COMPLETED";
                } else {
                    samplingStatus = "IN_PROGRESS";
                }
            }

            states.add(new PlotSamplingState(
                    plotId,
                    plotName,
                    variety,
                    areaHectares,
                    query.campaignYear(),
                    samplingStatus,
                    sampledTreesCount,
                    treesNeeded,
                    isRepresentative
            ));
        }

        states.sort(Comparator.comparing(PlotSamplingState::plotName));
        return Result.success(states);
    }
}

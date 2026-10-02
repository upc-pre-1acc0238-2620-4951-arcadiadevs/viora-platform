package com.arcadiadevs.viora.platform.thinning.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetSamplingSummaryQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.SamplingRoundSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetSamplingSummaryByPlotIdQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingDetailedResult;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingStatisticalSummary;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingTreeObservation;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import com.arcadiadevs.viora.platform.thinning.domain.services.SamplingCoverageEvaluator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Application query service implementation for field sampling coverage.
 */
@Service
@Transactional(readOnly = true)
public class GetSamplingSummaryQueryServiceImpl implements GetSamplingSummaryQueryService {

    private final FruitThinningPrescriptionRepository prescriptionRepository;
    private final ExternalOrchardService externalOrchardService;

    /**
     * Constructs the query service.
     *
     * @param prescriptionRepository repository port for thinning prescriptions
     * @param externalOrchardService outbound ACL for active plot verification
     */
    public GetSamplingSummaryQueryServiceImpl(
            FruitThinningPrescriptionRepository prescriptionRepository,
            ExternalOrchardService externalOrchardService
    ) {
        this.prescriptionRepository = prescriptionRepository;
        this.externalOrchardService = externalOrchardService;
    }

    @Override
    public Result<SamplingStatisticalSummary, ApplicationError> handle(GetSamplingSummaryByPlotIdQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "thinning.query.null"));
        }

        if (!externalOrchardService.existsActivePlot(query.plotId())) {
            return Result.failure(ApplicationError.notFound("Plot", query.plotId().plotId()));
        }

        var prescription = prescriptionRepository.findByPlotIdAndCampaignYear(
                query.plotId(),
                query.campaignYear()
        );

        return Result.success(toSummary(
                query,
                prescription.map(FruitThinningPrescription::snapshot).orElse(null)
        ));
    }

    @Override
    public Result<SamplingDetailedResult, ApplicationError> handleDetailed(GetSamplingSummaryByPlotIdQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "thinning.query.null"));
        }

        if (!externalOrchardService.existsActivePlot(query.plotId())) {
            return Result.failure(ApplicationError.notFound("Plot", query.plotId().plotId()));
        }

        var prescription = prescriptionRepository.findByPlotIdAndCampaignYear(
                query.plotId(),
                query.campaignYear()
        );

        FruitThinningPrescriptionSnapshot snapshot = prescription
                .map(FruitThinningPrescription::snapshot)
                .orElse(null);

        var summary = toSummary(query, snapshot);
        var observations = snapshot == null
                ? List.<SamplingTreeObservation>of()
                : toObservations(snapshot.samplingRounds());

        return Result.success(new SamplingDetailedResult(summary, observations));
    }

    private SamplingStatisticalSummary toSummary(
            GetSamplingSummaryByPlotIdQuery query,
            FruitThinningPrescriptionSnapshot snapshot
    ) {
        var rounds = snapshot == null ? List.<SamplingRoundSnapshot>of() : snapshot.samplingRounds();

        int uniqueTrees = SamplingCoverageEvaluator.countUniqueEvaluatedTreesFromSnapshots(rounds);
        int totalShoots = SamplingCoverageEvaluator.countTotalShootsFromSnapshots(rounds);
        double meanFruitsPerMeter = SamplingCoverageEvaluator.computeMeanFruitsPerMeterFromSnapshots(rounds);
        boolean representative = SamplingCoverageEvaluator.isRepresentativeFromSnapshots(rounds);
        int treesNeeded = SamplingCoverageEvaluator.treesNeededFromSnapshots(rounds);

        return new SamplingStatisticalSummary(
                query.plotId(),
                query.campaignYear(),
                uniqueTrees,
                totalShoots,
                meanFruitsPerMeter,
                representative,
                treesNeeded
        );
    }

    private List<SamplingTreeObservation> toObservations(List<SamplingRoundSnapshot> rounds) {
        var observations = new ArrayList<SamplingTreeObservation>();
        for (var round : rounds) {
            for (var record : round.samplingRecords()) {
                observations.add(new SamplingTreeObservation(
                        round.id(),
                        record.treeTag(),
                        record.shootFruitCount().shootCount(),
                        record.shootFruitCount().fruitSetCount(),
                        record.trunkCrossSectionalArea().trunkDiameterMm(),
                        record.samplingDate()
                ));
            }
        }
        observations.sort(Comparator
                .comparing(SamplingTreeObservation::samplingDate)
                .thenComparing(observation -> observation.roundId().roundId())
                .thenComparing(observation -> observation.treeTag().value()));
        return List.copyOf(observations);
    }
}

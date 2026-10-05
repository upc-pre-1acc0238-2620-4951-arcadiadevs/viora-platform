package com.arcadiadevs.viora.platform.thinning.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetThinningEventsQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.SamplingRoundSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetThinningEventsQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.ThinningEvent;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import com.arcadiadevs.viora.platform.thinning.domain.services.SamplingCoverageEvaluator;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform.ThinningRounding;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Application query service implementation retrieving thinning milestone events for the agronomic logbook.
 */
@Service
@Transactional(readOnly = true)
public class GetThinningEventsQueryServiceImpl implements GetThinningEventsQueryService {

    private final FruitThinningPrescriptionRepository prescriptionRepository;
    private final ExternalOrchardService externalOrchardService;

    /**
     * Constructs the query service.
     *
     * @param prescriptionRepository repository port for thinning prescriptions
     * @param externalOrchardService outbound ACL service for orchard context queries
     */
    public GetThinningEventsQueryServiceImpl(
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
    public Result<List<ThinningEvent>, ApplicationError> handle(GetThinningEventsQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "thinning.query.null"));
        }

        List<PlotId> targetPlotIds;
        if (query.plotId() != null) {
            if (!externalOrchardService.existsActivePlot(query.plotId())) {
                return Result.failure(ApplicationError.notFound("Plot", query.plotId().plotId()));
            }
            targetPlotIds = List.of(query.plotId());
        } else {
            targetPlotIds = externalOrchardService.findActivePlotIdsByProducerId(query.actorId());
            if (targetPlotIds.isEmpty()) {
                return Result.success(List.of());
            }
        }

        List<FruitThinningPrescription> prescriptions =
                prescriptionRepository.findByPlotIdInAndCampaignYear(targetPlotIds, query.campaignYear());

        List<ThinningEvent> events = new ArrayList<>();
        for (FruitThinningPrescription prescription : prescriptions) {
            FruitThinningPrescriptionSnapshot snap = prescription.snapshot();
            String plotName = externalOrchardService.findPlotName(snap.plotId()).orElse("Plot");

            // Milestone 1: Sampling completed
            var rounds = snap.samplingRounds();
            if (SamplingCoverageEvaluator.isRepresentativeFromSnapshots(rounds) && !rounds.isEmpty()) {
                SamplingRoundSnapshot representativeRound = rounds.stream()
                        .filter(r -> Boolean.TRUE.equals(r.isRepresentative()))
                        .findFirst()
                        .orElse(rounds.get(rounds.size() - 1));

                int uniqueTrees = SamplingCoverageEvaluator.countUniqueEvaluatedTreesFromSnapshots(rounds);
                int totalShoots = SamplingCoverageEvaluator.countTotalShootsFromSnapshots(rounds);
                int totalFruits = SamplingCoverageEvaluator.countTotalFruitsFromSnapshots(rounds);
                double rawMeanFruits = SamplingCoverageEvaluator.computeMeanFruitsPerShootFromSnapshots(rounds);
                Double roundedMeanFruits = ThinningRounding.load(rawMeanFruits);
                Instant occurredAt = representativeRound.createdAt() != null
                        ? representativeRound.createdAt()
                        : Instant.now();

                events.add(new ThinningEvent(
                        representativeRound.id().roundId(),
                        "SAMPLING_COMPLETED",
                        snap.id().prescriptionId(),
                        null,
                        snap.plotId(),
                        plotName,
                        snap.campaignYear(),
                        occurredAt,
                        uniqueTrees,
                        totalShoots,
                        totalFruits,
                        roundedMeanFruits,
                        true,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
            }

            // Milestone 2: Thinning executed
            var confirmation = snap.executionConfirmation();
            if (confirmation != null) {
                Instant occurredAt = confirmation.recordedAt() != null
                        ? confirmation.recordedAt()
                        : Instant.now();

                events.add(new ThinningEvent(
                        confirmation.id().confirmationId(),
                        "THINNING_EXECUTED",
                        snap.id().prescriptionId(),
                        confirmation.id().confirmationId(),
                        snap.plotId(),
                        plotName,
                        snap.campaignYear(),
                        occurredAt,
                        null,
                        null,
                        null,
                        null,
                        null,
                        confirmation.actualRemovalPercentage(),
                        confirmation.removedKg(),
                        confirmation.executionDate(),
                        confirmation.laborCrewSize(),
                        confirmation.timeliness() != null ? confirmation.timeliness().name() : null
                ));
            }
        }

        events.sort(Comparator.comparing(ThinningEvent::occurredAt).reversed());
        return Result.success(events);
    }
}

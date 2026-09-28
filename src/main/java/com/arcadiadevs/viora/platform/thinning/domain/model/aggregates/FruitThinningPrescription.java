package com.arcadiadevs.viora.platform.thinning.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.SamplingRound;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.SamplingRoundCompletedEvent;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.thinning.domain.services.FieldSamplingDeduplicator;
import com.arcadiadevs.viora.platform.thinning.domain.services.SamplingCoverageEvaluator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Root governing olive fruit thinning regulation, sampling rounds, and advisory prescriptions.
 */
public class FruitThinningPrescription extends AbstractDomainAggregateRoot<FruitThinningPrescription> {

    private final PrescriptionId id;
    private final PlotId plotId;
    private final CampaignYear campaignYear;
    private Long observedPlotRevision;
    private PrescriptionStatus status;
    private SustainableCropLoad sustainableLoad;
    private final List<SamplingRound> samplingRounds;
    private ExecutionConfirmationSnapshot executionConfirmation;
    private Long revision;

    private FruitThinningPrescription(
            PrescriptionId id,
            PlotId plotId,
            CampaignYear campaignYear,
            Long observedPlotRevision,
            PrescriptionStatus status,
            SustainableCropLoad sustainableLoad,
            List<SamplingRound> samplingRounds,
            ExecutionConfirmationSnapshot executionConfirmation,
            Long revision
    ) {
        this.id = id;
        this.plotId = plotId;
        this.campaignYear = campaignYear;
        this.observedPlotRevision = observedPlotRevision;
        this.status = status;
        this.sustainableLoad = sustainableLoad;
        this.samplingRounds = new ArrayList<>(samplingRounds);
        this.executionConfirmation = executionConfirmation;
        this.revision = revision;
    }

    /**
     * Domain factory method that initializes a new thinning prescription aggregate for a plot and campaign.
     *
     * @param plotId               the olive plot identifier
     * @param campaignYear         the monitored campaign year
     * @param observedPlotRevision the plot version at aggregate initialization
     * @return a consistent {@link FruitThinningPrescription} instance
     */
    public static FruitThinningPrescription createForPlot(
            PlotId plotId,
            CampaignYear campaignYear,
            Long observedPlotRevision
    ) {
        return new FruitThinningPrescription(
                new PrescriptionId(),
                plotId,
                campaignYear,
                observedPlotRevision,
                PrescriptionStatus.SAMPLING_IN_PROGRESS,
                SustainableCropLoad.empty(),
                new ArrayList<>(),
                null,
                0L
        );
    }

    /**
     * Factory method used by persistence layer to reconstitute an existing aggregate.
     *
     * @param snapshot the persistent state snapshot
     * @return a consistent {@link FruitThinningPrescription} instance
     */
    public static FruitThinningPrescription reconstitute(FruitThinningPrescriptionSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("thinning.prescription.snapshot.null");
        }
        List<SamplingRound> rounds = new ArrayList<>();
        for (SamplingRoundSnapshot roundSnapshot : snapshot.samplingRounds()) {
            rounds.add(SamplingRound.fromSnapshot(roundSnapshot));
        }
        return new FruitThinningPrescription(
                snapshot.id(),
                snapshot.plotId(),
                snapshot.campaignYear(),
                snapshot.observedPlotRevision(),
                snapshot.status(),
                snapshot.sustainableLoad(),
                rounds,
                snapshot.executionConfirmation(),
                snapshot.revision()
        );
    }

    /**
     * Ingests a new field sampling round batch conducted by a field technician or olive producer.
     *
     * @param actorId       the identifier of the sampling user
     * @param batchId       field sampling batch identifier
     * @param treeRecords   list of tree sampling records
     */
    public void ingestSamplingsBatch(
            UserId actorId,
            SamplingBatchId batchId,
            List<TreeSamplingRecord> treeRecords
    ) {
        // Deduplicate records in current batch
        List<TreeSamplingRecord> validatedRecords = FieldSamplingDeduplicator.validateNoDuplicates(treeRecords);

        // Check if batchId already exists in this aggregate
        for (SamplingRound existingRound : samplingRounds) {
            if (existingRound.batchId().equals(batchId)) {
                // Idempotent return without duplicate addition
                return;
            }
        }

        boolean wasRepresentativeBefore = SamplingCoverageEvaluator.isRepresentative(samplingRounds);

        SamplingRound newRound = SamplingRound.create(actorId, batchId, validatedRecords);
        samplingRounds.add(newRound);

        boolean isRepresentativeNow = SamplingCoverageEvaluator.isRepresentative(samplingRounds);
        newRound.setRepresentative(isRepresentativeNow);

        if (!wasRepresentativeBefore && isRepresentativeNow) {
            int uniqueTrees = SamplingCoverageEvaluator.countUniqueEvaluatedTrees(samplingRounds);
            registerDomainEvent(new SamplingRoundCompletedEvent(
                    id.prescriptionId(),
                    plotId.plotId(),
                    uniqueTrees,
                    Instant.now()
            ));
        }
    }

    /**
     * Returns an immutable snapshot representing the internal state of this aggregate.
     *
     * @return an immutable {@link FruitThinningPrescriptionSnapshot}
     */
    public FruitThinningPrescriptionSnapshot snapshot() {
        List<SamplingRoundSnapshot> roundSnapshots = new ArrayList<>();
        for (SamplingRound round : samplingRounds) {
            roundSnapshots.add(round.snapshot());
        }
        return new FruitThinningPrescriptionSnapshot(
                id,
                plotId,
                campaignYear,
                observedPlotRevision,
                status,
                sustainableLoad,
                roundSnapshots,
                executionConfirmation,
                revision
        );
    }
}

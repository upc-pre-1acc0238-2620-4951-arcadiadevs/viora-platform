package com.arcadiadevs.viora.platform.thinning.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.SamplingRound;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.SamplingRoundCompletedEvent;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.SustainableCropLoadDeterminedEvent;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.thinning.domain.services.CropLoadBalancingCalculatorService;
import com.arcadiadevs.viora.platform.thinning.domain.services.FieldSamplingDeduplicator;
import com.arcadiadevs.viora.platform.thinning.domain.services.SamplingCoverageEvaluator;

import java.time.Instant;
import java.time.Clock;
import java.util.UUID;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.ExecutionConfirmation;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.ThinningExecutionConfirmedEvent;
import java.time.LocalDate;
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
    private Instant issuedAt;
    private final List<SamplingRound> samplingRounds;
    private ExecutionConfirmationSnapshot executionConfirmation;
    private Long revision;
    private LocalDate fullBloomOn;

    private FruitThinningPrescription(
            PrescriptionId id,
            PlotId plotId,
            CampaignYear campaignYear,
            Long observedPlotRevision,
            PrescriptionStatus status,
            SustainableCropLoad sustainableLoad,
            Instant issuedAt,
            List<SamplingRound> samplingRounds,
            ExecutionConfirmationSnapshot executionConfirmation,
            Long revision,
            LocalDate fullBloomOn
    ) {
        this.id = id;
        this.plotId = plotId;
        this.campaignYear = campaignYear;
        this.observedPlotRevision = observedPlotRevision;
        this.status = status;
        this.sustainableLoad = sustainableLoad;
        this.issuedAt = issuedAt;
        this.samplingRounds = new ArrayList<>(samplingRounds);
        this.executionConfirmation = executionConfirmation;
        this.revision = revision;
        this.fullBloomOn = fullBloomOn;
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
                null,
                new ArrayList<>(),
                null,
                0L,
                null
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
                snapshot.issuedAt(),
                rounds,
                snapshot.executionConfirmation(),
                snapshot.revision(),
                snapshot.fullBloomOn()
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
        List<TreeSamplingRecord> validatedRecords = FieldSamplingDeduplicator.validateNoDuplicates(treeRecords);

        for (SamplingRound existingRound : samplingRounds) {
            if (existingRound.batchId().equals(batchId)) {
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
     * Determines the sustainable crop load after representative field sampling is available.
     *
     * @param calculator      pure domain calculator using the approved target of the plot variety
     * @param windowOpensOn   first recommended thinning date
     * @param windowClosesOn  latest recommended thinning date
     * @param profileVersion  version of the technical profile the target and the window come from
     * @param profileStatus   approval status of that profile
     */
    public void determineSustainableCropLoad(
            CropLoadBalancingCalculatorService calculator,
            LocalDate windowOpensOn,
            LocalDate windowClosesOn,
            String profileVersion,
            String profileStatus
    ) {
        if (calculator == null) {
            throw new IllegalArgumentException("thinning.calculator.null");
        }
        if (!SamplingCoverageEvaluator.isRepresentative(samplingRounds)) {
            throw new IllegalStateException("thinning.sampling.not_representative");
        }
        if (status == PrescriptionStatus.CONFIRMED || status == PrescriptionStatus.EXECUTED) {
            throw new IllegalStateException("thinning.prescription.already_confirmed");
        }

        double currentFruitsPerShoot = SamplingCoverageEvaluator.computeMeanFruitsPerShoot(samplingRounds);
        SustainableCropLoad calculatedLoad = calculator.calculate(
                currentFruitsPerShoot,
                windowOpensOn,
                windowClosesOn
        ).basedOn(profileVersion, profileStatus);

        this.sustainableLoad = calculatedLoad;
        this.status = PrescriptionStatus.PRESCRIBED;
        this.issuedAt = Instant.now();

        registerDomainEvent(new SustainableCropLoadDeterminedEvent(
                id.prescriptionId(),
                plotId.plotId(),
                calculatedLoad.percentageToRemove(),
                issuedAt
        ));
    }

    /**
     * Records the observed full bloom date of the campaign, the origin of the intervention window.
     *
     * @param observedOn date the plot reached full bloom
     * @param clock      clock used to reject future dates
     */
    public void recordFullBloom(LocalDate observedOn, Clock clock) {
        if (observedOn == null) {
            throw new IllegalArgumentException("thinning.full_bloom.null");
        }
        if (observedOn.getYear() != campaignYear.value()) {
            throw new IllegalArgumentException("thinning.full_bloom.campaign_mismatch");
        }
        if (observedOn.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("thinning.full_bloom.future");
        }
        if (status == PrescriptionStatus.CONFIRMED || status == PrescriptionStatus.EXECUTED) {
            throw new IllegalStateException("thinning.prescription.already_confirmed");
        }
        this.fullBloomOn = observedOn;
    }

    /**
     * Tells what still prevents the prescription from being issued.
     *
     * @param targetConfigured whether an approved profile gives the target load of the plot variety
     * @return the missing inputs, empty once the prescription was issued
     */
    public List<PrescriptionBlocker> blockers(boolean targetConfigured) {
        if (status != PrescriptionStatus.SAMPLING_IN_PROGRESS) {
            return List.of();
        }
        var missing = new ArrayList<PrescriptionBlocker>();
        if (!SamplingCoverageEvaluator.isRepresentative(samplingRounds)) {
            missing.add(PrescriptionBlocker.SAMPLING_NOT_REPRESENTATIVE);
        }
        if (!targetConfigured) {
            missing.add(PrescriptionBlocker.TARGET_NOT_CONFIGURED);
        }
        if (fullBloomOn == null) {
            missing.add(PrescriptionBlocker.FULL_BLOOM_MISSING);
        }
        return missing;
    }

    /** Records field execution using the current UTC date and no caliber calibration. */
    public void confirmExecution(LocalDate executionDate, double removedKg,
            double actualRemovalPercentage, int laborCrewSize, String notes) {
        confirmExecution(executionDate, removedKg, actualRemovalPercentage, laborCrewSize, notes,
                CaliberCalibration.none(), Clock.systemUTC());
    }

    /** Records field execution with an explicit clock and no caliber calibration. */
    public void confirmExecution(LocalDate executionDate, double removedKg,
            double actualRemovalPercentage, int laborCrewSize, String notes, Clock clock) {
        confirmExecution(executionDate, removedKg, actualRemovalPercentage, laborCrewSize, notes,
                CaliberCalibration.none(), clock);
    }

    /**
     * Records execution once, including late labor, together with the load balance it leaves and the
     * commercial caliber projection allowed by the calibration of the plot variety.
     *
     * @param executionDate           date the labor was completed
     * @param removedKg               removed biomass in kilograms
     * @param actualRemovalPercentage percentage of fruits actually removed
     * @param laborCrewSize           number of workers
     * @param notes                   optional field notes
     * @param calibration             caliber model calibration of the plot variety
     * @param clock                   clock used to reject future dates
     */
    public void confirmExecution(LocalDate executionDate, double removedKg,
            double actualRemovalPercentage, int laborCrewSize, String notes,
            CaliberCalibration calibration, Clock clock) {
        if (status != PrescriptionStatus.PRESCRIBED || executionConfirmation != null) {
            throw new IllegalStateException("thinning.prescription.not_prescribed");
        }
        if (sustainableLoad == null || sustainableLoad.windowClosesOn() == null
                || sustainableLoad.targetFruitsPerShoot() == null || sustainableLoad.percentageToRemove() == null) {
            throw new IllegalStateException("thinning.execution.window.missing");
        }
        if (executionDate != null && ((fullBloomOn != null && executionDate.isBefore(fullBloomOn))
                || (sustainableLoad.windowOpensOn() != null
                && executionDate.isBefore(sustainableLoad.windowOpensOn())))) {
            throw new IllegalArgumentException("thinning.execution.before_window");
        }
        var biomass = new RemovedBiomass(removedKg, actualRemovalPercentage);
        var crew = new LaborCrewSize(laborCrewSize);
        if (!SamplingCoverageEvaluator.isRepresentative(samplingRounds)) {
            throw new IllegalStateException("thinning.sampling.not_representative");
        }
        var loadBalance = LoadBalance.of(SamplingCoverageEvaluator.computeMeanFruitsPerShoot(samplingRounds),
                biomass.actualRemovalPercentage(), sustainableLoad.targetFruitsPerShoot());
        var confirmation = ExecutionConfirmation.create(executionDate, biomass, crew, notes,
                sustainableLoad.windowClosesOn(), loadBalance, calibration, clock);
        executionConfirmation = confirmation.snapshot();
        status = PrescriptionStatus.EXECUTED;
        registerDomainEvent(new ThinningExecutionConfirmedEvent(UUID.randomUUID().toString(),
                executionConfirmation.id().confirmationId(), id.prescriptionId(), plotId.plotId(),
                campaignYear.value(), executionDate, actualRemovalPercentage, removedKg, laborCrewSize,
                executionConfirmation.timeliness().name(), executionConfirmation.recordedAt(),
                sustainableLoad.percentageToRemove()));
    }

    /** Returns the immutable state for persistence and queries. */
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
                issuedAt,
                roundSnapshots,
                executionConfirmation,
                revision,
                fullBloomOn
        );
    }
}

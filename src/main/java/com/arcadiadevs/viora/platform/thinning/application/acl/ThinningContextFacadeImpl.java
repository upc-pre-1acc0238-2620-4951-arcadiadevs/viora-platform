package com.arcadiadevs.viora.platform.thinning.application.acl;

import com.arcadiadevs.viora.platform.thinning.application.internal.queryservices.ThinningPrescriptionOutlookService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberProjectionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CommercialSizeScale;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.YieldEstimate;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.YieldEstimateStatus;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import com.arcadiadevs.viora.platform.thinning.domain.services.SamplingCoverageEvaluator;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of the Thinning ACL facade, publishing the commercial size scale of the context to the contexts
 * that settle a harvest and have to name the caliber of the olives they delivered.
 *
 * <p>The label comes from {@link CommercialSizeScale}, the single owner of the scale, so no consumer has to know
 * how a caliber maps onto a grade.</p>
 */
@Service
public class ThinningContextFacadeImpl implements ThinningContextFacade {

    /** Lifecycle states in which the prescription has been issued to the producer. */
    private static final Set<PrescriptionStatus> ISSUED = EnumSet.of(
            PrescriptionStatus.PRESCRIBED, PrescriptionStatus.CONFIRMED, PrescriptionStatus.EXECUTED);

    private final FruitThinningPrescriptionRepository prescriptionRepository;
    private final ThinningPrescriptionOutlookService outlookService;

    public ThinningContextFacadeImpl(FruitThinningPrescriptionRepository prescriptionRepository,
            ThinningPrescriptionOutlookService outlookService) {
        this.prescriptionRepository = prescriptionRepository;
        this.outlookService = outlookService;
    }

    @Override
    public Optional<String> findCommercialSizeGrade(Double fruitsPerKg) {
        if (fruitsPerKg == null) {
            return Optional.empty();
        }
        return Optional.of(CommercialSizeScale.gradeOf(fruitsPerKg));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CampaignThinningPlan> findCampaignThinningPlan(String plotId, int campaignYear) {
        final PlotId id;
        final CampaignYear year;
        try {
            id = new PlotId(plotId);
            year = new CampaignYear(campaignYear);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        return prescriptionRepository.findByPlotIdAndCampaignYear(id, year).map(prescription -> {
            var snapshot = prescription.snapshot();
            var rounds = snapshot.samplingRounds();
            Double preThinning = SamplingCoverageEvaluator.isRepresentativeFromSnapshots(rounds)
                    ? SamplingCoverageEvaluator.computeMeanFruitsPerShootFromSnapshots(rounds)
                    : null;
            var load = snapshot.sustainableLoad();
            return new CampaignThinningPlan(
                    preThinning,
                    load == null ? null : load.targetFruitsPerShoot(),
                    load == null ? null : load.percentageToRemove(),
                    load == null ? null : load.windowOpensOn(),
                    load == null ? null : load.windowClosesOn());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PrescriptionAvailability> findPrescriptionAvailability(String plotId, int campaignYear) {
        final PlotId id;
        final CampaignYear year;
        try {
            id = new PlotId(plotId);
            year = new CampaignYear(campaignYear);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        return prescriptionRepository.findByPlotIdAndCampaignYear(id, year).map(prescription -> {
            var snapshot = prescription.snapshot();
            var rounds = snapshot.samplingRounds();
            var load = snapshot.sustainableLoad();
            boolean issued = ISSUED.contains(snapshot.status()) && load != null && load.percentageToRemove() != null;
            var confirmation = snapshot.executionConfirmation();
            var projection = confirmation == null ? null : confirmation.caliberProjection();
            return new PrescriptionAvailability(
                    issued,
                    issued ? load.percentageToRemove() : null,
                    SamplingCoverageEvaluator.countUniqueEvaluatedTreesFromSnapshots(rounds),
                    SamplingCoverageEvaluator.treesNeededFromSnapshots(rounds),
                    projection == null ? null : projection.status().name());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ThinningPlanDetail> findThinningPlanDetail(String plotId, int campaignYear) {
        final PlotId id;
        final CampaignYear year;
        try {
            id = new PlotId(plotId);
            year = new CampaignYear(campaignYear);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        return prescriptionRepository.findByPlotIdAndCampaignYear(id, year).map(prescription -> {
            var snapshot = prescription.snapshot();
            // Same outlook the thinning plan screen shows, so the assistant never reads a different figure
            var outlook = outlookService.outlookOf(snapshot);
            var load = snapshot.sustainableLoad();
            boolean issued = ISSUED.contains(snapshot.status()) && load != null && load.percentageToRemove() != null;
            var confirmation = snapshot.executionConfirmation();
            var projection = confirmation == null ? null : confirmation.caliberProjection();
            var estimated = projection != null && projection.status() == CaliberProjectionStatus.ESTIMATED;
            var yieldAfter = confirmation == null ? null : outlookService.yieldAfterThinning(snapshot);
            return new ThinningPlanDetail(
                    issued,
                    issued ? load.percentageToRemove() : null,
                    outlook.currentFruitsPerShoot(),
                    outlook.targetFruitsPerShoot(),
                    load == null ? null : load.windowOpensOn(),
                    load == null ? null : load.windowClosesOn(),
                    outlook.sampledTrees(),
                    outlook.lastSampledOn(),
                    confirmation == null ? null : confirmation.executionDate(),
                    projection == null ? null : projection.status().name(),
                    estimated ? projection.mostLikelyFruitsPerKg() : null,
                    estimated ? CommercialSizeScale.gradeOf(projection.mostLikelyFruitsPerKg()) : null,
                    yieldAfter == null ? null : yieldAfter.estimatedKgPerHa());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ThinningOutcome> findThinningOutcome(String plotId, int campaignYear) {
        final PlotId id;
        final CampaignYear year;
        try {
            id = new PlotId(plotId);
            year = new CampaignYear(campaignYear);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        return prescriptionRepository.findByPlotIdAndCampaignYear(id, year)
                .map(FruitThinningPrescription::snapshot)
                .filter(snapshot -> snapshot.executionConfirmation() != null)
                .map(snapshot -> {
                    var confirmation = snapshot.executionConfirmation();
                    var balance = confirmation.loadBalance();
                    // The same estimates the thinning plan screen shows: with the sampled load and with the residual
                    var outlook = outlookService.outlookOf(snapshot);
                    var without = available(outlook.yieldEstimate());
                    var after = available(outlookService.yieldAfterThinning(snapshot));
                    var load = snapshot.sustainableLoad();
                    return new ThinningOutcome(
                            confirmation.executionDate(),
                            confirmation.actualRemovalPercentage(),
                            confirmation.timeliness() == null ? null : confirmation.timeliness().name(),
                            balance == null ? null : balance.preThinningFruitsPerShoot(),
                            balance == null ? null : balance.residualFruitsPerShoot(),
                            balance == null ? null : balance.targetFruitsPerShoot(),
                            balance == null || balance.loadState() == null ? null : balance.loadState().name(),
                            without == null ? null : without.estimatedKgPerHa(),
                            after == null ? null : after.estimatedKgPerHa(),
                            after == null || after.basis() == null ? null : after.basis().name(),
                            after == null ? 0 : after.harvestCampaigns(),
                            load == null ? null : load.profileStatus(),
                            outlook.windowOpensDaysAfterBloom(),
                            outlook.windowClosesDaysAfterBloom());
                });
    }

    /** The estimate when it has a figure, or null. */
    private static YieldEstimate available(YieldEstimate estimate) {
        return estimate == null || estimate.status() != YieldEstimateStatus.AVAILABLE
                || estimate.estimatedKgPerHa() == null ? null : estimate;
    }
}

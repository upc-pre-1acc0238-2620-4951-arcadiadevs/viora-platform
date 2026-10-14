package com.arcadiadevs.viora.platform.thinning.application.internal.queryservices;

import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.ThinningPrescriptionIssuer;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.ThinningProfile;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.SamplingRoundSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.TreeSamplingRecordSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberCalibration;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.LoadBalance;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionOutlook;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.YieldEstimate;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.CaliberCalibrationObservationRepository;
import com.arcadiadevs.viora.platform.thinning.domain.services.CaliberModelFittingService;
import com.arcadiadevs.viora.platform.thinning.domain.services.SamplingCoverageEvaluator;
import com.arcadiadevs.viora.platform.thinning.domain.services.YieldEstimationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds what the thinning plan shows besides the prescription: the load before thinning against the target
 * (US26), the sampling progress, the profile offsets the full bloom sheet previews and the campaign yield.
 * It only reads; nothing is stored, so a later harvest or calibration is reflected on the next read.
 */
@Service
public class ThinningPrescriptionOutlookService {

    private final ExternalOrchardService externalOrchardService;
    private final ExternalPhenologyService externalPhenologyService;
    private final ThinningPrescriptionIssuer issuer;
    private final CaliberCalibrationObservationRepository observationRepository;

    public ThinningPrescriptionOutlookService(ExternalOrchardService externalOrchardService,
            ExternalPhenologyService externalPhenologyService, ThinningPrescriptionIssuer issuer,
            CaliberCalibrationObservationRepository observationRepository) {
        this.externalOrchardService = externalOrchardService;
        this.externalPhenologyService = externalPhenologyService;
        this.issuer = issuer;
        this.observationRepository = observationRepository;
    }

    /**
     * Builds the outlook of a prescription with the load measured by the sampling, before thinning.
     *
     * @param snapshot the prescription
     * @return its outlook
     */
    public PrescriptionOutlook outlookOf(FruitThinningPrescriptionSnapshot snapshot) {
        List<SamplingRoundSnapshot> rounds = snapshot.samplingRounds();
        Optional<ThinningProfile> profile = issuer.profileOf(snapshot.plotId());

        Double current = SamplingCoverageEvaluator.isRepresentativeFromSnapshots(rounds)
                ? SamplingCoverageEvaluator.computeMeanFruitsPerShootFromSnapshots(rounds)
                : null;
        Double target = snapshot.sustainableLoad() != null && snapshot.sustainableLoad().targetFruitsPerShoot() != null
                ? snapshot.sustainableLoad().targetFruitsPerShoot()
                : profile.map(ThinningProfile::targetFruitsPerShoot).orElse(null);
        LoadBalance balance = current == null || target == null ? null : LoadBalance.of(current, 0.0, target);

        return new PrescriptionOutlook(
                current,
                target,
                balance == null ? null : balance.loadRatio(),
                balance == null ? null : balance.loadState(),
                SamplingCoverageEvaluator.countUniqueEvaluatedTreesFromSnapshots(rounds),
                SamplingCoverageEvaluator.treesNeededFromSnapshots(rounds),
                lastSampledOn(rounds),
                profile.map(ThinningProfile::windowOpensDaysAfterBloom).orElse(null),
                profile.map(ThinningProfile::windowClosesDaysAfterBloom).orElse(null),
                yieldOf(snapshot, current, target));
    }

    /**
     * Estimates the campaign yield with the load left after the confirmed thinning.
     *
     * @param snapshot the prescription
     * @return the estimate, or null when no thinning was confirmed
     */
    public YieldEstimate yieldAfterThinning(FruitThinningPrescriptionSnapshot snapshot) {
        var confirmation = snapshot.executionConfirmation();
        if (confirmation == null || confirmation.loadBalance() == null) {
            return null;
        }
        LoadBalance balance = confirmation.loadBalance();
        return yieldOf(snapshot, balance.residualFruitsPerShoot(), balance.targetFruitsPerShoot());
    }

    private YieldEstimate yieldOf(FruitThinningPrescriptionSnapshot snapshot, Double load, Double target) {
        PlotId plotId = snapshot.plotId();
        return YieldEstimationService.estimate(
                externalPhenologyService.findHistoricalYields(plotId),
                snapshot.campaignYear().value(),
                externalOrchardService.findPlotAreaHectares(plotId).orElse(null),
                load,
                target,
                load == null || target == null ? CaliberCalibration.none() : calibrationFor(plotId));
    }

    /** Fits the caliber model of the plot variety from the real observations stored so far. */
    private CaliberCalibration calibrationFor(PlotId plotId) {
        return externalOrchardService.findPlotVariety(plotId)
                .map(variety -> CaliberModelFittingService.calibrate(variety,
                        observationRepository.findByVariety(variety)))
                .orElseGet(CaliberCalibration::none);
    }

    private static LocalDate lastSampledOn(List<SamplingRoundSnapshot> rounds) {
        if (rounds == null) {
            return null;
        }
        return rounds.stream()
                .flatMap(round -> round.samplingRecords().stream())
                .map(TreeSamplingRecordSnapshot::samplingDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
    }
}

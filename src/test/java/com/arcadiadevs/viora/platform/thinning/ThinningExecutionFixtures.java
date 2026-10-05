package com.arcadiadevs.viora.platform.thinning;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.*;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.*;
import java.time.*;
import java.util.*;

/** Issued prescription fixtures: issuance orchestration is not implemented by the upstream branch. */
public final class ThinningExecutionFixtures {
    /** Fruits per shoot measured by {@link #representativeRound()}: 84 fruits / 10 shoots. */
    public static final double SAMPLED_FRUITS_PER_SHOOT = 8.4;
    /** Sustainable target of the fixture prescriptions. */
    public static final double TARGET_FRUITS_PER_SHOOT = 6.0;

    private ThinningExecutionFixtures() { }

    public static FruitThinningPrescription prescribed(LocalDate closesOn) {
        return withStatus(PrescriptionStatus.PRESCRIBED, closesOn);
    }

    public static FruitThinningPrescription prescribed(LocalDate closesOn, String plotId) {
        return build(PrescriptionStatus.PRESCRIBED, closesOn, plotId, List.of(representativeRound()));
    }

    public static FruitThinningPrescription withStatus(PrescriptionStatus status, LocalDate closesOn) {
        return build(status, closesOn, UUID.randomUUID().toString(), List.of(representativeRound()));
    }

    public static FruitThinningPrescription prescribedWithoutSampling(LocalDate closesOn) {
        return build(PrescriptionStatus.PRESCRIBED, closesOn, UUID.randomUUID().toString(), List.of());
    }

    private static FruitThinningPrescription build(PrescriptionStatus status, LocalDate closesOn, String plotId,
            List<SamplingRoundSnapshot> rounds) {
        return FruitThinningPrescription.reconstitute(new FruitThinningPrescriptionSnapshot(
                new PrescriptionId(), new PlotId(plotId), new CampaignYear(2026),
                1L, status, new SustainableCropLoad(TARGET_FRUITS_PER_SHOOT, 25.0, closesOn),
                Instant.parse("2026-01-01T00:00:00Z"), rounds, null, 0L));
    }

    /** Five sampled trees with 10 shoots and 84 fruits each: representative, 8.4 fruits per shoot. */
    private static SamplingRoundSnapshot representativeRound() {
        var records = new ArrayList<TreeSamplingRecordSnapshot>();
        for (int tree = 1; tree <= 5; tree++) {
            records.add(new TreeSamplingRecordSnapshot(new SamplingRecordId(), new TreeTag("T-" + tree),
                    new ShootFruitCount(10, 84), new TrunkCrossSectionalArea(120.0), LocalDate.of(2025, 12, 1)));
        }
        return new SamplingRoundSnapshot(new RoundId(), new UserId(UUID.randomUUID().toString()),
                new SamplingBatchId(UUID.randomUUID().toString()), true, records);
    }

    /**
     * Noise-free calibration data generated from {@code W = 10 g x (L / 6)^-0.6}: eight on-time harvests of
     * four plots with residual loads between 3.6 and 9 fruits per shoot.
     */
    public static List<CaliberCalibrationObservation> exactObservations(String variety) {
        return observations(variety, new double[]{1, 1, 1, 1, 1, 1, 1, 1});
    }

    /** Same curve as {@link #exactObservations(String)} with a few percent of deterministic weight noise. */
    public static List<CaliberCalibrationObservation> noisyObservations(String variety) {
        return observations(variety, new double[]{1.04, 0.97, 1.02, 0.95, 1.05, 0.98, 1.01, 0.96});
    }

    private static List<CaliberCalibrationObservation> observations(String variety, double[] weightNoise) {
        double[] loads = {3.6, 4.4, 5.2, 6, 6.8, 7.6, 8.4, 9};
        String[] plots = {plotUuid(1), plotUuid(2), plotUuid(3), plotUuid(4)};
        var result = new ArrayList<CaliberCalibrationObservation>();
        for (int i = 0; i < loads.length; i++) {
            double grams = 10.0 * Math.pow(loads[i] / 6.0, -0.6) * weightNoise[i];
            result.add(new CaliberCalibrationObservation(new PlotId(plots[i % plots.length]),
                    new CampaignYear(2018 + i), variety, loads[i], 1000.0 / grams));
        }
        return result;
    }

    private static String plotUuid(int index) {
        return "00000000-0000-0000-0000-00000000000" + index;
    }
}

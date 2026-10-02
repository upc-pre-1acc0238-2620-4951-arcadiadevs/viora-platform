package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.util.Locale;

/**
 * One real data point used to calibrate the caliber model of a variety: the crop load left by an
 * on-time thinning and the commercial size the producer actually obtained at harvest.
 *
 * @param plotId                 plot where the campaign happened
 * @param campaignYear           harvested campaign
 * @param variety                olive variety of the plot (Orchard published name, e.g. SEVILLANA)
 * @param residualFruitsPerMeter crop load left after the on-time thinning of that campaign
 * @param commercialFruitsPerKg  fruits per kilogram reported at harvest settlement
 */
public record CaliberCalibrationObservation(
        PlotId plotId,
        CampaignYear campaignYear,
        String variety,
        double residualFruitsPerMeter,
        double commercialFruitsPerKg
) {

    public CaliberCalibrationObservation {
        if (plotId == null || campaignYear == null) {
            throw new IllegalArgumentException("thinning.calibration.reference.null");
        }
        if (variety == null || variety.isBlank()) {
            throw new IllegalArgumentException("thinning.calibration.variety.null_or_empty");
        }
        variety = variety.trim().toUpperCase(Locale.ROOT);
        if (!Double.isFinite(residualFruitsPerMeter) || residualFruitsPerMeter <= 0.0) {
            throw new IllegalArgumentException("thinning.calibration.load.invalid");
        }
        if (!Double.isFinite(commercialFruitsPerKg) || commercialFruitsPerKg <= 0.0) {
            throw new IllegalArgumentException("thinning.caliber.fruits_per_kg.invalid");
        }
    }

    /**
     * Returns the mean fruit fresh weight implied by the commercial size.
     *
     * @return grams per fruit, {@code 1000 / fruitsPerKg}
     */
    public double fruitWeightGrams() {
        return 1000.0 / commercialFruitsPerKg;
    }
}

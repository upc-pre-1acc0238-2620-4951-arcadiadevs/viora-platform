package com.arcadiadevs.viora.platform.thinning.domain.services;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberCalibration;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.YieldEstimate;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.YieldEstimateBasis;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.YieldEstimateStatus;

import java.util.List;
import java.util.Map;

/**
 * Pure domain service estimating the campaign yield of a plot from its own harvest history (US26).
 *
 * <p>Formulas, in kilograms per hectare like every yield of the platform (the app shows t/ha):</p>
 * <ul>
 *     <li>{@code sustainable = mean(kg of the earlier campaigns) / area}. An alternate bearing plot swings
 *     between ON and OFF years; the mean over the years is what its trees produce without exhausting their
 *     reserves, the yield of the sustainable target load.</li>
 *     <li>{@code estimated = sustainable x (L / T)^(1 - beta)}, where {@code L} is the current load and {@code T}
 *     the sustainable target, both in fruits per shoot. The yield grows with the number of fruits ({@code L / T})
 *     while the fruit gets smaller as the load grows ({@code W ~ L^-beta}, the caliber model of the variety).</li>
 * </ul>
 *
 * <p>No coefficient is invented: {@code beta} is used only when the caliber model of the variety is calibrated
 * with real harvests and covers both loads. Otherwise {@code beta = 0} and the estimate follows the fruit count
 * alone, marked {@link YieldEstimateBasis#FRUIT_COUNT_ONLY}; above the target it is a ceiling.</p>
 */
public final class YieldEstimationService {

    /** Earlier harvested campaigns needed, the same history the alternate bearing index asks for. */
    public static final int MIN_HARVEST_CAMPAIGNS = 3;


    private YieldEstimationService() {
    }

    /**
     * Estimates the yield of a campaign.
     *
     * @param harvestKgByYear  total kilograms harvested per campaign year on the plot
     * @param campaignYear     campaign being estimated; only earlier campaigns form the history
     * @param areaHectares     plot area, or null when unknown
     * @param currentLoad      current fruits per shoot, or null when unknown
     * @param targetLoad       sustainable target in fruits per shoot, or null when the variety has none
     * @param calibration      caliber calibration of the plot variety
     * @return the estimate, or the reason it could not be made
     */
    public static YieldEstimate estimate(Map<Integer, Double> harvestKgByYear, int campaignYear, Double areaHectares,
            Double currentLoad, Double targetLoad, CaliberCalibration calibration) {
        var earlier = harvestKgByYear == null ? List.<Double>of()
                : harvestKgByYear.entrySet().stream()
                        .filter(entry -> entry.getKey() != null && entry.getKey() < campaignYear)
                        .map(Map.Entry::getValue)
                        .filter(kg -> kg != null && Double.isFinite(kg) && kg >= 0.0)
                        .toList();
        int campaigns = earlier.size();
        if (areaHectares == null || !Double.isFinite(areaHectares) || areaHectares <= 0.0) {
            return YieldEstimate.unavailable(YieldEstimateStatus.AREA_UNKNOWN, campaigns);
        }
        if (campaigns < MIN_HARVEST_CAMPAIGNS) {
            return YieldEstimate.unavailable(YieldEstimateStatus.INSUFFICIENT_HISTORY, campaigns);
        }
        double meanKg = earlier.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double sustainable = meanKg / areaHectares;

        if (!isLoad(currentLoad) || !isLoad(targetLoad) || targetLoad == 0.0) {
            return new YieldEstimate(YieldEstimateStatus.LOAD_UNKNOWN, sustainable, null, null, campaigns, 0.0);
        }
        boolean caliberAdjusted = calibration != null && calibration.isCalibrated()
                && currentLoad > 0.0 && calibration.model().covers(currentLoad) && calibration.model().covers(targetLoad);
        double beta = caliberAdjusted ? calibration.model().beta() : 0.0;
        double estimated = sustainable * Math.pow(currentLoad / targetLoad, 1.0 - beta);
        return new YieldEstimate(YieldEstimateStatus.AVAILABLE, sustainable, estimated,
                caliberAdjusted ? YieldEstimateBasis.CALIBER_ADJUSTED : YieldEstimateBasis.FRUIT_COUNT_ONLY,
                campaigns, beta);
    }

    private static boolean isLoad(Double value) {
        return value != null && Double.isFinite(value) && value >= 0.0;
    }
}

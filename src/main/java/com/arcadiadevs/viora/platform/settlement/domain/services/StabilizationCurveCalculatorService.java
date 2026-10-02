package com.arcadiadevs.viora.platform.settlement.domain.services;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.StabilizationStatus;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.StabilizationTrendCurve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Pure domain service computing the interannual stabilization curve of a plot.
 *
 * <ul>
 *     <li><b>Alternation index</b> (Hoblyn, 1936) over pairs of consecutive campaigns only:
 *     {@code I = mean(|Y(t+1) - Y(t)| / (Y(t+1) + Y(t)))}. A missing year breaks the pair instead of
 *     comparing two non-consecutive campaigns.</li>
 *     <li><b>Baseline</b>: Phenology history of campaigns before the first settlement; managed period:
 *     settled campaigns. Each index needs at least {@value #MIN_CONSECUTIVE_PAIRS} consecutive pairs.</li>
 *     <li><b>Amplitude reduction rate</b>: {@code ARR = (I_baseline - I_managed) / I_baseline}, a fraction;
 *     negative when alternation grew. The stabilization target is {@code ARR >= }{@value #TARGET_REDUCTION}.</li>
 *     <li><b>Variance</b>: sample variance (n - 1) of settled yields in kg^2, plus the coefficient of
 *     variation (sample standard deviation / mean) to compare plots of different size.</li>
 * </ul>
 */
public final class StabilizationCurveCalculatorService {

    public static final int MIN_CONSECUTIVE_PAIRS = 2;
    public static final double TARGET_REDUCTION = 0.30;

    private StabilizationCurveCalculatorService() {
    }

    /**
     * Computes the curve for the settled campaigns of a plot.
     *
     * @param historicalYields total kg per campaign registered in Phenology (any order of insertion)
     * @param settledYields    total kg per settled campaign, including the campaign being closed
     * @return the stabilization curve
     */
    public static StabilizationTrendCurve computeCurve(SortedMap<Integer, Double> historicalYields,
            SortedMap<Integer, Double> settledYields) {
        if (settledYields == null || settledYields.isEmpty()) {
            throw new IllegalArgumentException("settlement.curve.settlements.empty");
        }
        SortedMap<Integer, Double> baseline = historicalYields == null
                ? Collections.emptySortedMap()
                : historicalYields.headMap(settledYields.firstKey());

        Double baselineYield = baseline.isEmpty() ? null : mean(new ArrayList<>(baseline.values()));
        Double baselineIndex = alternationIndex(baseline);
        Double managedIndex = alternationIndex(settledYields);
        Double variance = null;
        Double coefficientOfVariation = null;
        if (settledYields.size() >= 2) {
            List<Double> settled = new ArrayList<>(settledYields.values());
            variance = sampleVariance(settled);
            coefficientOfVariation = Math.sqrt(variance) / mean(settled);
        }

        StabilizationStatus status;
        Double reductionRate = null;
        Boolean targetAchieved = null;
        if (baselineIndex == null) {
            status = StabilizationStatus.INSUFFICIENT_BASELINE;
        } else if (managedIndex == null) {
            status = StabilizationStatus.INSUFFICIENT_SETTLEMENTS;
        } else if (baselineIndex == 0.0) {
            status = StabilizationStatus.NO_BASELINE_ALTERNATION;
        } else {
            status = StabilizationStatus.EVALUATED;
            reductionRate = (baselineIndex - managedIndex) / baselineIndex;
            targetAchieved = reductionRate >= TARGET_REDUCTION;
        }
        return new StabilizationTrendCurve(status, baseline.size(), settledYields.size(), baselineYield,
                baselineIndex, managedIndex, reductionRate, targetAchieved, variance, coefficientOfVariation);
    }

    /**
     * Hoblyn alternation index over pairs of consecutive campaigns.
     *
     * @param yields total kg per campaign
     * @return the index in [0, 1], or {@code null} with fewer than {@value #MIN_CONSECUTIVE_PAIRS} pairs
     */
    static Double alternationIndex(SortedMap<Integer, Double> yields) {
        double sum = 0.0;
        int pairs = 0;
        Map.Entry<Integer, Double> previous = null;
        for (Map.Entry<Integer, Double> current : yields.entrySet()) {
            if (previous != null && current.getKey() == previous.getKey() + 1) {
                double total = previous.getValue() + current.getValue();
                if (total > 0.0) {
                    sum += Math.abs(current.getValue() - previous.getValue()) / total;
                    pairs++;
                }
            }
            previous = current;
        }
        return pairs < MIN_CONSECUTIVE_PAIRS ? null : sum / pairs;
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).sum() / values.size();
    }

    private static double sampleVariance(List<Double> values) {
        double mean = mean(values);
        double squares = 0.0;
        for (double value : values) {
            squares += (value - mean) * (value - mean);
        }
        return squares / (values.size() - 1);
    }
}

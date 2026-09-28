package com.arcadiadevs.viora.platform.phenology.domain.services;

import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.HistoricalHarvestEntry;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.BbiAlternationCategory;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.BiennialBearingIndex;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.SampleSufficiency;

import java.util.Comparator;
import java.util.List;

/**
 * Pure domain service evaluating Hoblyn's Biennial Bearing Index (BBI, 1936)
 * from an ordered chronological series of historical harvest yields:
 *
 * <pre>
 *   BBI = (1 / (N - 1)) * SUM_{i=1}^{N-1} [ |Y_{i+1} - Y_i| / (Y_{i+1} + Y_i) ]
 * </pre>
 *
 * If fewer than 2 distinct campaign years are present, returns BBI = 0.0 (insufficient data).
 */
public final class HoblynBbiCalculatorService {

    private HoblynBbiCalculatorService() {
    }

    /**
     * Evaluates sample sufficiency according to domain requirements (minimum 3 campaigns).
     *
     * @param sampleSize the number of evaluated harvest campaigns
     * @return the {@link SampleSufficiency} assessment
     */
    public static SampleSufficiency evaluateSufficiency(int sampleSize) {
        return SampleSufficiency.of(sampleSize);
    }

    /**
     * Calculates the Biennial Bearing Index (Hoblyn BBI) for the given list of harvest entries.
     *
     * @param entries the list of historical harvest records
     * @return a {@link BiennialBearingIndex} value object in range [0.00, 1.00]
     */
    public static BiennialBearingIndex calculateBbi(List<HistoricalHarvestEntry> entries) {
        if (entries == null || entries.size() < 2) {
            return BiennialBearingIndex.zero();
        }

        var sortedEntries = entries.stream()
                .sorted(Comparator.comparing(e -> e.snapshot().campaignYear().value()))
                .toList();

        int n = sortedEntries.size();
        double sum = 0.0;
        int validPairs = 0;

        for (int i = 0; i < n - 1; i++) {
            double y1 = sortedEntries.get(i).snapshot().harvestYield().totalKg();
            double y2 = sortedEntries.get(i + 1).snapshot().harvestYield().totalKg();

            double denominator = y1 + y2;
            if (denominator > 0.0) {
                sum += Math.abs(y2 - y1) / denominator;
                validPairs++;
            }
        }

        if (validPairs == 0) {
            return BiennialBearingIndex.zero();
        }

        double bbiValue = sum / validPairs;
        // Clamp to precision bounds [0.00, 1.00]
        double clamped = Math.max(0.00, Math.min(1.00, Math.round(bbiValue * 1000.0) / 1000.0));
        return new BiennialBearingIndex(clamped);
    }

    /**
     * Determines the qualitative alternation category according to Hoblyn BBI agronomic thresholds.
     *
     * @param bbi        the calculated BBI value object
     * @param sampleSize the number of evaluated harvest campaigns
     * @return the corresponding {@link BbiAlternationCategory}
     */
    public static BbiAlternationCategory classifyAlternation(
            BiennialBearingIndex bbi,
            int sampleSize
    ) {
        if (bbi == null || sampleSize < 2) {
            return BbiAlternationCategory.INSUFFICIENT_DATA;
        }
        double val = bbi.value();
        if (val < 0.25) {
            return BbiAlternationCategory.REGULAR;
        } else if (val <= 0.50) {
            return BbiAlternationCategory.MODERATE_ALTERNATION;
        } else {
            return BbiAlternationCategory.SEVERE_ALTERNATION;
        }
    }
}


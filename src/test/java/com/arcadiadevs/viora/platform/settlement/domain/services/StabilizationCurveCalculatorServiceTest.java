package com.arcadiadevs.viora.platform.settlement.domain.services;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.StabilizationStatus;
import org.junit.jupiter.api.Test;

import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class StabilizationCurveCalculatorServiceTest {

    /** Baseline 2022-2025 alternates strongly: 10000, 2000, 9000, 3000 kg. */
    static SortedMap<Integer, Double> history() {
        return yields(2022, 10000, 2000, 9000, 3000);
    }

    static SortedMap<Integer, Double> yields(int firstYear, double... kilograms) {
        var map = new TreeMap<Integer, Double>();
        for (int i = 0; i < kilograms.length; i++) {
            map.put(firstYear + i, kilograms[i]);
        }
        return map;
    }

    @Test
    void evaluatesTheAmplitudeReductionAgainstTheBaseline() {
        var curve = StabilizationCurveCalculatorService.computeCurve(history(), yields(2026, 7000, 5000, 6500));
        // Baseline I = (8000/12000 + 7000/11000 + 6000/12000) / 3; managed I = (2000/12000 + 1500/11500) / 2
        double baseline = (8000.0 / 12000 + 7000.0 / 11000 + 6000.0 / 12000) / 3;
        double managed = (2000.0 / 12000 + 1500.0 / 11500) / 2;
        assertEquals(StabilizationStatus.EVALUATED, curve.status());
        assertEquals(4, curve.baselineCampaigns());
        assertEquals(3, curve.settledCampaigns());
        assertEquals(6000.0, curve.baselineYieldKg(), 1e-9);
        assertEquals(baseline, curve.baselineAlternationIndex(), 1e-12);
        assertEquals(managed, curve.managedAlternationIndex(), 1e-12);
        assertEquals((baseline - managed) / baseline, curve.amplitudeReductionRate(), 1e-12);
        assertEquals(0.7528, curve.amplitudeReductionRate(), 1e-4);
        assertTrue(curve.targetAchieved());
    }

    @Test
    void reportsANegativeReductionWhenAlternationGrows() {
        var curve = StabilizationCurveCalculatorService.computeCurve(yields(2022, 6000, 5000, 6000),
                yields(2026, 9000, 2000, 9500));
        assertEquals(StabilizationStatus.EVALUATED, curve.status());
        assertTrue(curve.amplitudeReductionRate() < 0.0);
        assertFalse(curve.targetAchieved());
    }

    @Test
    void requiresTwoConsecutivePairsOfBaselineCampaigns() {
        var curve = StabilizationCurveCalculatorService.computeCurve(yields(2024, 9000, 3000),
                yields(2026, 7000, 5000, 6500));
        assertEquals(StabilizationStatus.INSUFFICIENT_BASELINE, curve.status());
        assertNull(curve.baselineAlternationIndex());
        assertNull(curve.amplitudeReductionRate());
        assertNull(curve.targetAchieved());
        assertEquals(6000.0, curve.baselineYieldKg(), 1e-9);
    }

    @Test
    void requiresTwoConsecutivePairsOfSettledCampaigns() {
        var curve = StabilizationCurveCalculatorService.computeCurve(history(), yields(2026, 7000, 5000));
        assertEquals(StabilizationStatus.INSUFFICIENT_SETTLEMENTS, curve.status());
        assertNull(curve.managedAlternationIndex());
        assertNull(curve.amplitudeReductionRate());
        // Sample variance of 7000 and 5000 is 2 000 000 kg^2; CV = 1414.21 / 6000
        assertEquals(2_000_000.0, curve.interannualVarianceKg2(), 1e-6);
        assertEquals(Math.sqrt(2_000_000.0) / 6000.0, curve.coefficientOfVariation(), 1e-12);
    }

    @Test
    void aMissingYearBreaksTheConsecutivePair() {
        var settled = new TreeMap<Integer, Double>();
        settled.put(2026, 7000.0);
        settled.put(2028, 5000.0);
        settled.put(2029, 6500.0);
        var curve = StabilizationCurveCalculatorService.computeCurve(history(), settled);
        assertEquals(StabilizationStatus.INSUFFICIENT_SETTLEMENTS, curve.status());
    }

    @Test
    void doesNotClaimAReductionWithoutBaselineAlternation() {
        var curve = StabilizationCurveCalculatorService.computeCurve(yields(2022, 5000, 5000, 5000),
                yields(2026, 7000, 5000, 6500));
        assertEquals(StabilizationStatus.NO_BASELINE_ALTERNATION, curve.status());
        assertEquals(0.0, curve.baselineAlternationIndex());
        assertNull(curve.amplitudeReductionRate());
    }

    @Test
    void ignoresHistoryFromTheSettledPeriodToAvoidCountingAHarvestTwice() {
        var history = history();
        history.put(2026, 1.0);
        history.put(2027, 50000.0);
        var curve = StabilizationCurveCalculatorService.computeCurve(history, yields(2026, 7000));
        assertEquals(4, curve.baselineCampaigns());
        assertEquals(6000.0, curve.baselineYieldKg(), 1e-9);
        assertNull(curve.interannualVarianceKg2());
        assertNull(curve.coefficientOfVariation());
    }

    @Test
    void requiresAtLeastOneSettledCampaign() {
        assertThrows(IllegalArgumentException.class,
                () -> StabilizationCurveCalculatorService.computeCurve(history(), new TreeMap<>()));
    }
}

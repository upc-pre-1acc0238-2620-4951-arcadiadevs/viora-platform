package com.arcadiadevs.viora.platform.thinning.domain.services;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberCalibration;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.VarietyCaliberModel;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.YieldEstimateBasis;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.YieldEstimateStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Yield estimation (US26)")
class YieldEstimationServiceTest {

    private static final int CAMPAIGN = 2026;
    /** 2023-2025 average 20 500 kg on 2.5 ha: 8 200 kg/ha. */
    private static final Map<Integer, Double> HISTORY = new TreeMap<>(Map.of(2023, 24_000.0, 2024, 16_000.0, 2025, 21_500.0));

    /** A calibrated Sevillana with beta 0.5 that covers loads from 0.2 to 0.8 fruits per shoot. */
    private static CaliberCalibration calibrated() {
        return CaliberCalibration.calibrated(
                new VarietyCaliberModel("SEVILLANA", 5.0, 0.5, 0.1, 10, 3, Math.log(0.4), 2.0, 0.2, 0.8));
    }

    @Test
    @DisplayName("Sustainable is the mean of the earlier campaigns per hectare; estimated follows the fruit count")
    void fruitCountOnlyWithoutCalibration() {
        var estimate = YieldEstimationService.estimate(HISTORY, CAMPAIGN, 2.5, 0.57, 0.40, CaliberCalibration.none());

        assertEquals(YieldEstimateStatus.AVAILABLE, estimate.status());
        assertEquals(8200.0, estimate.sustainableKgPerHa(), 1e-9);
        assertEquals(8200.0 * 0.57 / 0.40, estimate.estimatedKgPerHa(), 1e-9);
        assertEquals(YieldEstimateBasis.FRUIT_COUNT_ONLY, estimate.basis());
        assertEquals(3, estimate.harvestCampaigns());
        assertEquals(0.0, estimate.caliberElasticity());
    }

    @Test
    @DisplayName("A calibrated variety shrinks the fruit as the load grows: (L / T)^(1 - beta)")
    void caliberAdjustedWhenCalibrated() {
        var estimate = YieldEstimationService.estimate(HISTORY, CAMPAIGN, 2.5, 0.57, 0.40, calibrated());

        assertEquals(YieldEstimateBasis.CALIBER_ADJUSTED, estimate.basis());
        assertEquals(8200.0 * Math.pow(0.57 / 0.40, 0.5), estimate.estimatedKgPerHa(), 1e-9);
        assertEquals(0.5, estimate.caliberElasticity());
    }

    @Test
    @DisplayName("A load outside the calibrated range falls back to the fruit count")
    void outsideTheCalibratedRangeFallsBack() {
        var estimate = YieldEstimationService.estimate(HISTORY, CAMPAIGN, 2.5, 0.95, 0.40, calibrated());

        assertEquals(YieldEstimateBasis.FRUIT_COUNT_ONLY, estimate.basis());
        assertEquals(8200.0 * 0.95 / 0.40, estimate.estimatedKgPerHa(), 1e-9);
    }

    @Test
    @DisplayName("At the target load the estimate equals the sustainable yield")
    void targetLoadGivesTheSustainableYield() {
        var estimate = YieldEstimationService.estimate(HISTORY, CAMPAIGN, 2.5, 0.40, 0.40, calibrated());

        assertEquals(estimate.sustainableKgPerHa(), estimate.estimatedKgPerHa(), 1e-9);
    }

    @Test
    @DisplayName("Only campaigns before the estimated one count, and three are needed")
    void needsThreeEarlierCampaigns() {
        var history = new TreeMap<>(Map.of(2024, 16_000.0, 2025, 21_500.0, 2026, 30_000.0, 2027, 1.0));

        var estimate = YieldEstimationService.estimate(history, CAMPAIGN, 2.5, 0.57, 0.40, CaliberCalibration.none());

        assertEquals(YieldEstimateStatus.INSUFFICIENT_HISTORY, estimate.status());
        assertEquals(2, estimate.harvestCampaigns());
        assertNull(estimate.sustainableKgPerHa());
        assertNull(estimate.estimatedKgPerHa());
    }

    @Test
    @DisplayName("Without the current load only the sustainable yield is known")
    void loadUnknownKeepsTheSustainableYield() {
        var withoutLoad = YieldEstimationService.estimate(HISTORY, CAMPAIGN, 2.5, null, 0.40, CaliberCalibration.none());
        var withoutTarget = YieldEstimationService.estimate(HISTORY, CAMPAIGN, 2.5, 0.57, null, CaliberCalibration.none());

        assertEquals(YieldEstimateStatus.LOAD_UNKNOWN, withoutLoad.status());
        assertEquals(8200.0, withoutLoad.sustainableKgPerHa(), 1e-9);
        assertNull(withoutLoad.estimatedKgPerHa());
        assertNull(withoutLoad.basis());
        assertEquals(YieldEstimateStatus.LOAD_UNKNOWN, withoutTarget.status());
    }

    @Test
    @DisplayName("Without a usable area nothing is estimated")
    void areaUnknown() {
        assertEquals(YieldEstimateStatus.AREA_UNKNOWN,
                YieldEstimationService.estimate(HISTORY, CAMPAIGN, null, 0.57, 0.40, CaliberCalibration.none()).status());
        assertEquals(YieldEstimateStatus.AREA_UNKNOWN,
                YieldEstimationService.estimate(HISTORY, CAMPAIGN, 0.0, 0.57, 0.40, CaliberCalibration.none()).status());
    }

    @Test
    @DisplayName("No fruit left means no yield")
    void noFruitNoYield() {
        var estimate = YieldEstimationService.estimate(HISTORY, CAMPAIGN, 2.5, 0.0, 0.40, CaliberCalibration.none());

        assertEquals(0.0, estimate.estimatedKgPerHa(), 1e-9);
    }
}

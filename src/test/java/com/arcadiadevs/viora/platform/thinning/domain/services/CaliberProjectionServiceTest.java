package com.arcadiadevs.viora.platform.thinning.domain.services;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.arcadiadevs.viora.platform.thinning.ThinningExecutionFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class CaliberProjectionServiceTest {
    private static final CaliberCalibration EXACT =
            CaliberModelFittingService.calibrate("SEVILLANA", exactObservations("SEVILLANA"));
    private static final CaliberCalibration NOISY =
            CaliberModelFittingService.calibrate("SEVILLANA", noisyObservations("SEVILLANA"));

    @Test
    void estimatesTheCaliberOfAnOnTimeLaborInsideTheCalibratedRange() {
        // 42 fruits/m with 35% removal leaves 27.3 fruits/m: W = 10 x (27.3/30)^-0.6 = 10.58 g -> 94.5 fruits/kg
        var projection = CaliberProjectionService.project(LoadBalance.of(42, 35, 30), ExecutionTimeliness.OPTIMAL, EXACT);
        assertEquals(CaliberProjectionStatus.ESTIMATED, projection.status());
        assertEquals(94.5, projection.mostLikelyFruitsPerKg(), 0.05);
        assertEquals("91/100", projection.mostLikelySizeGrade());
        // Noise-free data: the interval collapses onto the estimate (up to floating-point error)
        assertEquals(projection.mostLikelyFruitsPerKg(), projection.fruitsPerKgLow(), 1e-6);
        assertEquals(projection.mostLikelyFruitsPerKg(), projection.fruitsPerKgHigh(), 1e-6);
        assertEquals(0.80, projection.confidenceLevel());
        assertEquals(8, projection.calibrationObservations());
        assertEquals(CaliberProjection.MODEL_VERSION, projection.modelVersion());
    }

    @Test
    void reportsAPredictionIntervalWhenTheDataHaveNoise() {
        var projection = CaliberProjectionService.project(LoadBalance.of(42, 35, 30), ExecutionTimeliness.OPTIMAL, NOISY);
        assertEquals(CaliberProjectionStatus.ESTIMATED, projection.status());
        assertTrue(projection.fruitsPerKgLow() < projection.mostLikelyFruitsPerKg());
        assertTrue(projection.mostLikelyFruitsPerKg() < projection.fruitsPerKgHigh());
        assertNotNull(projection.sizeGradeLow());
        assertNotNull(projection.sizeGradeHigh());
    }

    @Test
    void gradesTheUnroundedEstimateNearAScaleBoundary() {
        // Residual load chosen so the curve gives 100.46 fruits/kg: rounding to 100.5 first would wrongly yield 101/110
        double residual = 30.0 * Math.pow(1000.0 / 100.46 / 10.0, -1.0 / 0.6);
        var projection = CaliberProjectionService.project(LoadBalance.of(residual, 0, 30), ExecutionTimeliness.OPTIMAL, EXACT);
        assertEquals(100.46, projection.mostLikelyFruitsPerKg(), 1e-6);
        assertEquals("91/100", projection.mostLikelySizeGrade());
    }

    @Test
    void neverEstimatesWithoutFruitLeft() {
        var projection = CaliberProjectionService.project(LoadBalance.of(42, 100, 30), ExecutionTimeliness.LATE, EXACT);
        assertWithoutFigures(projection, CaliberProjectionStatus.NOT_APPLICABLE);
    }

    @Test
    void neverEstimatesLateLabor() {
        var projection = CaliberProjectionService.project(LoadBalance.of(42, 35, 30), ExecutionTimeliness.LATE, EXACT);
        assertWithoutFigures(projection, CaliberProjectionStatus.NOT_ESTIMATED_LATE);
    }

    @Test
    void neverEstimatesAnUncalibratedVariety() {
        var calibration = CaliberCalibration.uncalibrated("CRIOLLA", 3);
        var projection = CaliberProjectionService.project(LoadBalance.of(42, 35, 30), ExecutionTimeliness.OPTIMAL, calibration);
        assertWithoutFigures(projection, CaliberProjectionStatus.NOT_CALIBRATED);
        assertEquals(3, projection.calibrationObservations());
        assertWithoutFigures(CaliberProjectionService.project(LoadBalance.of(42, 35, 30), ExecutionTimeliness.OPTIMAL, null),
                CaliberProjectionStatus.NOT_CALIBRATED);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 60})
    void neverExtrapolatesOutsideTheObservedLoadRange(double removal) {
        // Residual loads of 46 and 16.8 fruits/m lie outside the observed range [18, 45]
        double pre = removal == 0 ? 46 : 42;
        var projection = CaliberProjectionService.project(LoadBalance.of(pre, removal, 30), ExecutionTimeliness.OPTIMAL, EXACT);
        assertWithoutFigures(projection, CaliberProjectionStatus.OUTSIDE_CALIBRATION_RANGE);
    }

    private static void assertWithoutFigures(CaliberProjection projection, CaliberProjectionStatus status) {
        assertEquals(status, projection.status());
        assertNull(projection.mostLikelyFruitsPerKg());
        assertNull(projection.fruitsPerKgLow());
        assertNull(projection.fruitsPerKgHigh());
        assertNull(projection.confidenceLevel());
        assertNull(projection.mostLikelySizeGrade());
    }
}

package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalanceTest {

    @ParameterizedTest
    @CsvSource({
            "42, 25, 31.5, 1.5, 1.05, MODERATE_OVERLOAD",
            "42, 35, 27.3, -2.7, 0.91, BALANCED",
            "42, 0, 42.0, 12.0, 1.4, SEVERE_OVERLOAD",
            "42, 100, 0.0, -30.0, 0.0, BALANCED"
    })
    void computesResidualLoadDeltaRatioAndState(double pre, double removal, double residual, double delta,
            double ratio, LoadState state) {
        var balance = LoadBalance.of(pre, removal, 30.0);
        assertEquals(pre, balance.preThinningFruitsPerShoot());
        assertEquals(residual, balance.residualFruitsPerShoot(), 1e-9);
        assertEquals(30.0, balance.targetFruitsPerShoot());
        assertEquals(delta, balance.deltaFruitsPerShoot(), 1e-9);
        assertEquals(ratio, balance.loadRatio(), 1e-9);
        assertEquals(state, balance.loadState());
    }

    @ParameterizedTest
    @CsvSource({"0.0, BALANCED", "1.0, BALANCED", "1.001, MODERATE_OVERLOAD", "1.3, MODERATE_OVERLOAD",
            "1.301, SEVERE_OVERLOAD"})
    void classifiesRatiosWithTheThirtyPercentOverloadRule(double ratio, LoadState expected) {
        assertEquals(expected, LoadState.fromRatio(ratio));
    }

    @ParameterizedTest
    @CsvSource({"30.01, MODERATE_OVERLOAD", "39.01, SEVERE_OVERLOAD", "30.0, BALANCED", "39.0, MODERATE_OVERLOAD"})
    void decidesTheStateWithFullPrecisionInsteadOfRoundedValues(double residual, LoadState expected) {
        var balance = LoadBalance.of(residual, 0, 30.0);
        assertEquals(expected, balance.loadState());
        assertEquals(residual / 30.0, balance.loadRatio());
    }

    @Test
    void keepsATinyResidualLoadInsteadOfRoundingItToZero() {
        var balance = LoadBalance.of(42, 99.999, 30);
        assertTrue(balance.residualFruitsPerShoot() > 0.0);
        assertEquals(42 * (1 - 99.999 / 100), balance.residualFruitsPerShoot());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, 100.1, Double.NaN})
    void rejectsInvalidRemovalPercentage(double removal) {
        assertThrows(IllegalArgumentException.class, () -> LoadBalance.of(42, removal, 30));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -5, Double.POSITIVE_INFINITY})
    void rejectsInvalidTarget(double target) {
        assertThrows(IllegalArgumentException.class, () -> LoadBalance.of(42, 25, target));
    }

    @Test
    void rejectsNegativeLoads() {
        assertThrows(IllegalArgumentException.class, () -> LoadBalance.of(-1, 25, 30));
        assertThrows(IllegalArgumentException.class, () -> LoadState.fromRatio(-0.1));
    }
}

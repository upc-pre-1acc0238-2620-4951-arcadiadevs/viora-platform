package com.arcadiadevs.viora.platform.thinning.domain.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CropLoadBalancingCalculatorServiceTest {

    private static final LocalDate OPENS = LocalDate.of(2026, 10, 29);
    private static final LocalDate CLOSES = LocalDate.of(2026, 12, 3);

    @Test
    @DisplayName("Removes what brings the load down to the target: 100 x (1 - target / load)")
    void removesTheExcessOverTheTarget() {
        var load = new CropLoadBalancingCalculatorService(0.4).calculate(0.8, OPENS, CLOSES);

        assertThat(load.percentageToRemove()).isEqualTo(50.0);
        assertThat(load.targetFruitsPerShoot()).isEqualTo(0.4);
        assertThat(load.windowOpensOn()).isEqualTo(OPENS);
        assertThat(load.windowClosesOn()).isEqualTo(CLOSES);
    }

    @Test
    @DisplayName("Keeps full precision: only the REST layer rounds")
    void keepsFullPrecision() {
        var load = new CropLoadBalancingCalculatorService(0.4).calculate(0.6, OPENS, CLOSES);

        assertThat(load.percentageToRemove()).isEqualTo(100.0 * (1.0 - 0.4 / 0.6));
    }

    @Test
    @DisplayName("A load at or below the target needs no thinning")
    void nothingToRemoveAtOrBelowTheTarget() {
        var calculator = new CropLoadBalancingCalculatorService(0.4);

        assertThat(calculator.calculate(0.4, OPENS, CLOSES).percentageToRemove()).isZero();
        assertThat(calculator.calculate(0.3, OPENS, CLOSES).percentageToRemove()).isZero();
        assertThat(calculator.calculate(0.0, OPENS, CLOSES).percentageToRemove()).isZero();
    }

    @Test
    @DisplayName("Applies no coefficient besides the target: the BBI plays no part")
    void appliesNoOtherCoefficient() {
        var load = new CropLoadBalancingCalculatorService(1.0).calculate(2.0, OPENS, CLOSES);

        assertThat(load.percentageToRemove()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Rejects a target that is not positive and a load that is negative")
    void rejectsInvalidNumbers() {
        assertThatThrownBy(() -> new CropLoadBalancingCalculatorService(0.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CropLoadBalancingCalculatorService(0.4).calculate(-1.0, OPENS, CLOSES))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** A tree sample cannot carry counts or sizes no field could produce: they would overflow the plan or break it. */
class SamplingInputBoundsTest {

    @Test
    void acceptsTheLargestRealisticCounts() {
        var count = new ShootFruitCount(ShootFruitCount.MAX_SHOOT_COUNT, ShootFruitCount.MAX_FRUIT_SET_COUNT);
        assertEquals(100.0, count.fruitsPerShoot());
    }

    @Test
    void rejectsCountsAboveTheirCeiling() {
        var shoots = assertThrows(IllegalArgumentException.class, () -> new ShootFruitCount(1001, 10));
        assertEquals("thinning.shoot_count.too_large", shoots.getMessage());
        var fruits = assertThrows(IllegalArgumentException.class, () -> new ShootFruitCount(10, 2_000_000_000));
        assertEquals("thinning.fruit_count.too_large", fruits.getMessage());
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.POSITIVE_INFINITY, Double.NaN, 0.0, -1.0, 3000.5})
    void rejectsTrunkDiametersNoTreeHas(double millimetres) {
        assertThrows(IllegalArgumentException.class, () -> new TrunkCrossSectionalArea(millimetres));
    }

    @Test
    void rejectsATreeTagLongerThanItsColumn() {
        assertEquals("T-01", new TreeTag(" T-01 ").value());
        assertThrows(IllegalArgumentException.class, () -> new TreeTag("T".repeat(51)));
    }
}

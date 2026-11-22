package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A weighed handful of fruits gives the caliber, and only a caliber an olive can have. */
class FruitSampleTest {

    @Test
    void theCaliberIsTheFruitsInAKilogram() {
        assertEquals(200.0, new FruitSample(50, 250).fruitsPerKg());
        assertEquals(333.3, new FruitSample(10, 30).fruitsPerKg());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 250, thinning.fruit_sample.count.invalid",
            "1001, 250, thinning.fruit_sample.count.invalid",
            "50, 0, thinning.fruit_sample.weight.invalid",
            "50, -3, thinning.fruit_sample.weight.invalid",
            "50, 10001, thinning.fruit_sample.weight.invalid",
            "50, 1000, thinning.fruit_sample.caliber.out_of_range",
            "500, 200, thinning.fruit_sample.caliber.out_of_range"
    })
    void refusesWhatNoFieldCouldWeigh(int fruits, double grams, String reason) {
        var error = assertThrows(IllegalArgumentException.class, () -> new FruitSample(fruits, grams));
        assertEquals(reason, error.getMessage());
    }
}

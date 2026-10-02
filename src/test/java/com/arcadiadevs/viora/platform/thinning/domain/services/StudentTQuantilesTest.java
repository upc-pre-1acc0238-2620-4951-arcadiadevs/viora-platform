package com.arcadiadevs.viora.platform.thinning.domain.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class StudentTQuantilesTest {

    @ParameterizedTest
    @CsvSource({"1, 3.078, 6.314", "6, 1.440, 1.943", "30, 1.310, 1.697"})
    void readsTheStandardTable(int df, double p90, double p95) {
        assertEquals(p90, StudentTQuantiles.p90(df), 1e-9);
        assertEquals(p95, StudentTQuantiles.p95(df), 1e-9);
    }

    @ParameterizedTest
    @CsvSource({"40, 1.303, 1.684", "60, 1.296, 1.671", "120, 1.289, 1.658"})
    void approximatesLargeDegreesOfFreedomWithinOneThousandth(int df, double p90, double p95) {
        assertEquals(p90, StudentTQuantiles.p90(df), 1e-3);
        assertEquals(p95, StudentTQuantiles.p95(df), 1e-3);
    }

    @Test
    void rejectsNonPositiveDegreesOfFreedom() {
        assertThrows(IllegalArgumentException.class, () -> StudentTQuantiles.p90(0));
        assertThrows(IllegalArgumentException.class, () -> StudentTQuantiles.p95(-1));
    }
}

package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Grades follow the IOC COI/OT/NC no. 1 (2004) size scale, section 2.5. */
class CommercialSizeScaleTest {

    @ParameterizedTest
    @CsvSource({
            "59.4, <60",
            "60, 60/70",
            "70, 60/70",
            "70.6, 71/80",
            "94.5, 91/100",
            "120, 111/120",
            "121, 121/140",
            "200, 181/200",
            "201, 201/230",
            "410, 381/410",
            "411, 411/460",
            "461, 461/510",
            "512, 511/560"
    })
    void gradesFruitsPerKilogramWithTheIocScale(double fruitsPerKg, String grade) {
        assertEquals(grade, CommercialSizeScale.gradeOf(fruitsPerKg));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -10, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidCounts(double fruitsPerKg) {
        assertThrows(IllegalArgumentException.class, () -> CommercialSizeScale.gradeOf(fruitsPerKg));
    }
}

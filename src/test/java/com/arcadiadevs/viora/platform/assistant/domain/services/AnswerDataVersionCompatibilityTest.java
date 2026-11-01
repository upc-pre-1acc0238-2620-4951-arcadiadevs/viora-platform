package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AlternationFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlanFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the data version of the screens already in production. The saved answers keep the version they were written
 * from, so a different recipe for the same figures would turn every saved answer stale at once.
 */
@DisplayName("Data version: the saved answers of production stay up to date")
class AnswerDataVersionCompatibilityTest {

    @Test
    @DisplayName("ALTERNATION keeps the version of its figures, with and without a history")
    void alternation() {
        var yields = new TreeMap<Integer, Double>();
        yields.put(2023, 40_000.0);
        yields.put(2024, 100_000.0);
        yields.put(2025, 50_000.0);
        var withHistory = new AlternationFacts(2026, 10.0, yields, 0.50, "MODERATE_ALTERNATION", false);
        var withoutHistory = new AlternationFacts(2026, null, new TreeMap<>(), null, null, false);

        assertThat(AnswerComposer.dataVersionOf(ScreenKey.ALTERNATION, withHistory).value())
                .isEqualTo("sha256:f0360dc88fcf9630c97bf34d04dfdfd4dfd4e43555b3d8b98ecbfea0aa38be66");
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.ALTERNATION, withoutHistory).value())
                .isEqualTo("sha256:4ced5ed56e1a47d288051e08e05f89d19f77c282304dd6899bfbc14da4657ffa");
    }

    @Test
    @DisplayName("PLAN keeps the version of its figures, with and without a prescription")
    void plan() {
        var withPrescription = new PlanFacts(2026, 0.60, 0.40, 30.0, LocalDate.of(2026, 11, 3),
                LocalDate.of(2026, 11, 14), 5, false, null, 0);
        var withoutPrescription = new PlanFacts(2026, null, null, null, null, null, 0, false, null, 0);

        assertThat(AnswerComposer.dataVersionOf(ScreenKey.PLAN, withPrescription).value())
                .isEqualTo("sha256:340ed7f020c4ab2215684d01135b0b12587de499d646d4957f631e0d8dd04aee");
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.PLAN, withoutPrescription).value())
                .isEqualTo("sha256:8779049a17f282f498aa295c8ba2663f4236e0e615a2ce23dd1993d5b8dfc4e8");
    }
}

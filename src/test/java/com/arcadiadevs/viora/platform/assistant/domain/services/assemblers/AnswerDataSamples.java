package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;

import java.time.LocalDate;
import java.util.SortedMap;
import java.util.TreeMap;

/** Plot data of the assembler and template tests: mockup-like figures, not validated by an agronomist. */
public final class AnswerDataSamples {

    public static final PlotId PLOT = new PlotId("550e8400-e29b-41d4-a716-446655440001");
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    private AnswerDataSamples() {
    }

    /** Three harvests: 9600, 3100 and 8300 kg. */
    public static SortedMap<Integer, Double> yields() {
        var yields = new TreeMap<Integer, Double>();
        yields.put(2023, 9600.0);
        yields.put(2024, 3100.0);
        yields.put(2025, 8300.0);
        return yields;
    }

    /** Issued plan: 0.5714 fruits per shoot against 0.40, remove 30 %, window 20 Oct - 10 Nov, 5 trees. */
    public static ThinningPlanData issuedPlan() {
        return plan(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 10), null);
    }

    public static ThinningPlanData plan(LocalDate opensOn, LocalDate closesOn, LocalDate executedOn) {
        return new ThinningPlanData(true, 30.0, 0.5714, 0.4, opensOn, closesOn, 5, LocalDate.of(2026, 10, 29),
                executedOn, null, null, null, null);
    }

    /** Thinning confirmed with an estimated caliber of 180.4 fruits per kilo and 8 213.6 kg/ha. */
    public static ThinningPlanData thinnedPlan(Double estimatedKgPerHa) {
        return new ThinningPlanData(true, 30.0, 0.5714, 0.4, LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 11, 10), 5, LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 28),
                "ESTIMATED", 180.4, "171/190", estimatedKgPerHa);
    }

    public static PlotAnswerData alternation(ThinningPlanData plan) {
        return new PlotAnswerData(PLOT, TODAY, 2026, 0.5123, "MODERATE_ALTERNATION", yields(), plan);
    }

    public static PlotAnswerData alternation(SortedMap<Integer, Double> yields) {
        return new PlotAnswerData(PLOT, TODAY, 2026, 0.5123, "MODERATE_ALTERNATION", yields, null);
    }

    public static PlotAnswerData plan(ThinningPlanData plan) {
        return new PlotAnswerData(PLOT, TODAY, 2026, null, null, null, plan);
    }
}

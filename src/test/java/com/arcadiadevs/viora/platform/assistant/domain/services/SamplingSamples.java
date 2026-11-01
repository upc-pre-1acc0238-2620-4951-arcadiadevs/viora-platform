package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.SamplingFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;

import java.time.LocalDate;

/** Samplings of a plot as the sampling summary screen reads them, judged on October 31, 2026. */
final class SamplingSamples {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    private SamplingSamples() {
    }

    /** The screen figures for a plan and the trees still missing. */
    static SamplingFacts sampling(ThinningPlanData plan, int treesNeeded) {
        return new SamplingFacts(TODAY, plan, treesNeeded);
    }

    /** Contract example: 5/5 trees, 0.59 fruits per shoot against 0.40, remove 32 %, window open until November 11. */
    static ThinningPlanData representative() {
        return issued(0.59, 0.40, 32.0, LocalDate.of(2026, 10, 21), LocalDate.of(2026, 11, 11), null);
    }

    /** Load already at the target: 0.38 against 0.40, remove 0 %. */
    static ThinningPlanData within() {
        return issued(0.38, 0.40, 0.0, LocalDate.of(2026, 10, 21), LocalDate.of(2026, 11, 11), null);
    }

    /** The window opens on November 5 and closes on November 25. */
    static ThinningPlanData beforeWindow() {
        return issued(0.59, 0.40, 32.0, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 25), null);
    }

    /** The window closed on October 20 without a recorded thinning. */
    static ThinningPlanData closedWindow() {
        return issued(0.59, 0.40, 32.0, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), null);
    }

    /** The thinning was recorded on October 25. */
    static ThinningPlanData done() {
        return issued(0.59, 0.40, 32.0, LocalDate.of(2026, 10, 21), LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 10, 25));
    }

    /** Sampling halfway, without the prescription: 3 trees with 0.57 fruits per shoot. */
    static ThinningPlanData halfway(int trees) {
        return new ThinningPlanData(false, null, 0.57, null, null, null, trees, TODAY, null, null, null, null, null);
    }

    /** Representative sampling whose prescription is not issued yet. */
    static ThinningPlanData unissued() {
        return new ThinningPlanData(false, null, 0.57, null, null, null, 5, LocalDate.of(2026, 10, 30), null, null,
                null, null, null);
    }

    static ThinningPlanData issued(double load, double target, double percent, LocalDate opens, LocalDate closes,
                                   LocalDate executedOn) {
        return new ThinningPlanData(true, percent, load, target, opens, closes, 5, LocalDate.of(2026, 10, 30),
                executedOn, null, null, null, null);
    }
}

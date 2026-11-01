package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningDoneFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningOutcomeData;

import java.time.LocalDate;

/**
 * Outcomes of a thinning confirmed on 28 October 2026, campaign 2026. The contract figures: 0.57 fruits per shoot
 * before it, 30 % removed, 0.40 left against a target of 0.40, 8,200 kg/ha estimated against 11,700 kg/ha without
 * thinning (counting fruits only), a demonstration profile whose window runs 14 to 49 days after bloom.
 */
final class ThinningDoneSamples {

    static final int CAMPAIGN = 2026;
    static final LocalDate EXECUTED_ON = LocalDate.of(2026, 10, 28);

    private ThinningDoneSamples() {
    }

    static ThinningDoneFacts thinningDone(ThinningOutcomeData outcome) {
        return new ThinningDoneFacts(CAMPAIGN, outcome);
    }

    static ThinningDoneFacts notRecorded() {
        return new ThinningDoneFacts(CAMPAIGN, null);
    }

    /** The contract outcome; every other sample changes one or two of its figures. */
    static Outcome contract() {
        return new Outcome();
    }

    /** Mutable copy of the contract outcome, so each test changes only the figures it is about. */
    static final class Outcome {
        Double removed = 30.0;
        String timeliness = "OPTIMAL";
        Double pre = 0.5714;
        Double residual = 0.4;
        Double target = 0.4;
        String loadState = "BALANCED";
        Double withoutThinning = 11700.0;
        Double afterThinning = 8200.0;
        String yieldBasis = ThinningOutcomeData.FRUIT_COUNT_ONLY;
        int harvests = 3;
        String profileStatus = "SYNTHETIC_DEMO";
        Integer opens = 14;
        Integer closes = 49;

        Outcome removed(Double value) {
            removed = value;
            return this;
        }

        Outcome load(Double preValue, Double residualValue, Double targetValue, String state) {
            pre = preValue;
            residual = residualValue;
            target = targetValue;
            loadState = state;
            return this;
        }

        Outcome yield(Double without, Double after, String basis) {
            withoutThinning = without;
            afterThinning = after;
            yieldBasis = basis;
            return this;
        }

        Outcome profile(String status) {
            profileStatus = status;
            return this;
        }

        Outcome window(Integer opensDays, Integer closesDays) {
            opens = opensDays;
            closes = closesDays;
            return this;
        }

        ThinningOutcomeData build() {
            return new ThinningOutcomeData(EXECUTED_ON, removed, timeliness, pre, residual, target, loadState,
                    withoutThinning, afterThinning, yieldBasis, harvests, profileStatus, opens, closes);
        }

        ThinningDoneFacts facts() {
            return thinningDone(build());
        }
    }
}

package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CampaignClosedFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningOutcomeData;

import java.time.LocalDate;
import java.util.TreeMap;

/** Closed campaign figures of a plot whose latest recorded harvest is the 2025 campaign. */
final class CampaignClosedSamples {

    static final LocalDate EXECUTED = LocalDate.of(2025, 11, 3);

    private CampaignClosedSamples() {
    }

    /** Harvests from {@code firstYear} on, one per year, in kilograms. */
    static TreeMap<Integer, Double> harvests(int firstYear, double... kilograms) {
        var yields = new TreeMap<Integer, Double>();
        for (int i = 0; i < kilograms.length; i++) {
            yields.put(firstYear + i, kilograms[i]);
        }
        return yields;
    }

    /** The contract figures: 5.8, 6.1 and 6.4 t/ha before and 8.3 t/ha in 2025 over 1.5 ha. */
    static TreeMap<Integer, Double> contractHarvests() {
        return harvests(2022, 8700.0, 9150.0, 9600.0, 12450.0);
    }

    static CampaignClosedFacts closed(TreeMap<Integer, Double> yields, Double index, String category,
                                      ThinningOutcomeData outcome, Double areaHectares) {
        return new CampaignClosedFacts(yields, index, category, outcome, areaHectares);
    }

    /** Only the harvests: no index, no thinning, no area. */
    static CampaignClosedFacts harvestsOnly(TreeMap<Integer, Double> yields) {
        return closed(yields, null, null, null, null);
    }

    /** The confirmed thinning of 3 November 2025 with its timeliness and the window of the variety profile. */
    static ThinningOutcomeData outcome(String timeliness, Integer opens, Integer closes) {
        return new ThinningOutcomeData(EXECUTED, 30.0, timeliness, 0.57, 0.4, 0.4, "BALANCED", 9100.0, 8200.0,
                "CALIBER_ADJUSTED", 3, "PROVISIONAL", opens, closes);
    }
}

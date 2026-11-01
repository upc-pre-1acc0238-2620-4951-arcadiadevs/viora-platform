package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * Outcome of the confirmed thinning of a plot campaign, as Thinning computes it, for the THINNING_DONE and
 * CAMPAIGN_CLOSED questions.
 *
 * @param executedOn                 day the thinning was done
 * @param actualRemovalPercentage    percentage of fruits the producer reported removing; null when not reported
 * @param timeliness                 {@code OPTIMAL} or {@code LATE}; null when unknown
 * @param preThinningLoad            fruits per shoot measured before the thinning; null when unknown
 * @param residualLoad               fruits per shoot left after it; null when unknown
 * @param targetLoad                 sustainable target, fruits per shoot; null when unknown
 * @param loadState                  {@code BALANCED}, {@code MODERATE_OVERLOAD} or {@code SEVERE_OVERLOAD} of the
 *                                   residual load; null when unknown
 * @param kgPerHaWithoutThinning     yield estimated with the load before the thinning; null when not estimated
 * @param kgPerHaAfterThinning       yield estimated with the residual load; null when not estimated
 * @param yieldBasis                 {@code CALIBER_ADJUSTED} or {@code FRUIT_COUNT_ONLY}; null without estimate
 * @param harvestCampaigns           earlier harvests the estimate averages
 * @param profileStatus              {@code AGRONOMIST_APPROVED}, {@code PROVISIONAL} or {@code SYNTHETIC_DEMO} of the
 *                                   profile the target comes from; null when unknown
 * @param windowOpensDaysAfterBloom  days after full bloom the window opens in that profile; null when unknown
 * @param windowClosesDaysAfterBloom days after full bloom it closes; null when unknown
 */
public record ThinningOutcomeData(
        LocalDate executedOn,
        Double actualRemovalPercentage,
        String timeliness,
        Double preThinningLoad,
        Double residualLoad,
        Double targetLoad,
        String loadState,
        Double kgPerHaWithoutThinning,
        Double kgPerHaAfterThinning,
        String yieldBasis,
        int harvestCampaigns,
        String profileStatus,
        Integer windowOpensDaysAfterBloom,
        Integer windowClosesDaysAfterBloom
) {

    /** Basis of a yield that follows the number of fruits alone: with more fruit than the target it is a ceiling. */
    public static final String FRUIT_COUNT_ONLY = "FRUIT_COUNT_ONLY";

    public ThinningOutcomeData {
        if (executedOn == null) {
            throw new IllegalArgumentException("assistant.thinning_outcome.invalid");
        }
    }

    String canonical() {
        return executedOn + ";" + actualRemovalPercentage + ";" + timeliness + ";" + preThinningLoad + ";"
                + residualLoad + ";" + targetLoad + ";" + loadState + ";" + kgPerHaWithoutThinning + ";"
                + kgPerHaAfterThinning + ";" + yieldBasis + ";" + harvestCampaigns + ";" + profileStatus + ";"
                + windowOpensDaysAfterBloom + ";" + windowClosesDaysAfterBloom;
    }
}

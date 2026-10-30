package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * Thinning plan of the current campaign as the thinning context computed it. Every field is null when unknown.
 *
 * @param issued                        whether the prescription was issued
 * @param percentageToRemove            prescribed percentage of fruits to remove
 * @param preThinningFruitsPerShoot     mean load of the representative sampling, before thinning
 * @param targetFruitsPerShoot          sustainable target of the prescription
 * @param windowOpensOn                 first recommended thinning date
 * @param windowClosesOn                latest recommended thinning date, before pit hardening
 * @param sampledTrees                  distinct trees sampled
 * @param lastSampledOn                 date of the latest tree sampled
 * @param executedOn                    date the thinning was confirmed; null while not confirmed
 * @param caliberProjectionStatus       status of the caliber projection of the confirmed thinning
 * @param mostLikelyFruitsPerKg         central caliber estimate, only when estimated
 * @param commercialSizeGrade           commercial size grade of that caliber
 * @param estimatedKgPerHaAfterThinning campaign yield estimated with the load left by the thinning
 */
public record ThinningPlanData(
        boolean issued,
        Double percentageToRemove,
        Double preThinningFruitsPerShoot,
        Double targetFruitsPerShoot,
        LocalDate windowOpensOn,
        LocalDate windowClosesOn,
        int sampledTrees,
        LocalDate lastSampledOn,
        LocalDate executedOn,
        String caliberProjectionStatus,
        Double mostLikelyFruitsPerKg,
        String commercialSizeGrade,
        Double estimatedKgPerHaAfterThinning
) {
}

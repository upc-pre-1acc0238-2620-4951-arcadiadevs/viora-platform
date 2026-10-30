package com.arcadiadevs.viora.platform.thinning.interfaces.acl;

import java.time.LocalDate;
import java.util.Optional;

/**
 * ACL facade exposing Thinning bounded context capabilities to external contexts.
 *
 * <p>Provides a clean integration surface without leaking Thinning domain aggregates, internal value objects, or
 * persistence entities to consuming bounded contexts.</p>
 */
public interface ThinningContextFacade {

    /**
     * Retrieves the commercial size grade of a caliber expressed as number of fruits per kilogram.
     *
     * <p>The grade is the label of the International Olive Council size scale the caliber falls into, for example
     * {@code "91/100"}.</p>
     *
     * @param fruitsPerKg number of fruits per kilogram; may be null when the caliber is unknown
     * @return Optional containing the grade label, or empty if the caliber is absent
     */
    Optional<String> findCommercialSizeGrade(Double fruitsPerKg);

    /**
     * Retrieves the crop load and the thinning prescription of a plot campaign, for the agronomic dossier.
     *
     * @param plotId       the plot identifier string (UUID)
     * @param campaignYear the campaign year
     * @return the plan of the campaign, or empty when the plot was never sampled that campaign
     */
    Optional<CampaignThinningPlan> findCampaignThinningPlan(String plotId, int campaignYear);

    /**
     * Crop load and prescription of a campaign, in fruits per sampled shoot. Every field is null when unknown.
     *
     * @param preThinningFruitsPerShoot mean load of the representative sampling, before thinning
     * @param targetFruitsPerShoot      sustainable target of the issued prescription
     * @param percentageToRemove        prescribed percentage of fruits to remove
     * @param windowOpensOn             first recommended thinning date
     * @param windowClosesOn            latest recommended thinning date
     */
    record CampaignThinningPlan(
            Double preThinningFruitsPerShoot,
            Double targetFruitsPerShoot,
            Double percentageToRemove,
            LocalDate windowOpensOn,
            LocalDate windowClosesOn
    ) {
    }

    /**
     * Retrieves how far the thinning prescription of a plot campaign got: whether it was issued, the sampling
     * still missing and the caliber projection of its confirmed execution.
     *
     * @param plotId       the plot identifier string (UUID)
     * @param campaignYear the campaign year
     * @return the availability, or empty when the plot was never sampled that campaign
     */
    Optional<PrescriptionAvailability> findPrescriptionAvailability(String plotId, int campaignYear);

    /**
     * Availability of a campaign prescription.
     *
     * @param issued                  whether the prescription was issued (prescribed, confirmed or executed)
     * @param percentageToRemove      prescribed percentage of fruits to remove; null while not issued
     * @param sampledTrees            distinct trees sampled so far
     * @param treesNeeded             trees still missing for a representative sampling; 0 once representative
     * @param caliberProjectionStatus status of the caliber projection of the confirmed execution (for example
     *                                {@code ESTIMATED}); null while the thinning is not confirmed
     */
    record PrescriptionAvailability(
            boolean issued,
            Double percentageToRemove,
            int sampledTrees,
            int treesNeeded,
            String caliberProjectionStatus
    ) {
    }

    /**
     * Retrieves what the assistant needs to explain the thinning plan of a plot campaign: the load measured by the
     * sampling against the target, the window, the sampling behind it and, once the thinning was confirmed, the
     * caliber projection and the yield estimated with the load left. Every figure is the one the thinning plan
     * already shows; nothing is recomputed here.
     *
     * @param plotId       the plot identifier string (UUID)
     * @param campaignYear the campaign year
     * @return the plan detail, or empty when the plot was never sampled that campaign
     */
    Optional<ThinningPlanDetail> findThinningPlanDetail(String plotId, int campaignYear);

    /**
     * Thinning plan of a campaign as the assistant explains it. Every field is null when unknown.
     *
     * @param issued                        whether the prescription was issued (prescribed, confirmed or executed)
     * @param percentageToRemove            prescribed percentage of fruits to remove; null while not issued
     * @param preThinningFruitsPerShoot     mean load of the representative sampling, before thinning
     * @param targetFruitsPerShoot          sustainable target of the prescription
     * @param windowOpensOn                 first recommended thinning date
     * @param windowClosesOn                latest recommended thinning date
     * @param sampledTrees                  distinct trees sampled so far
     * @param lastSampledOn                 date of the latest tree sampled
     * @param executedOn                    date the thinning was confirmed as done; null while not confirmed
     * @param caliberProjectionStatus       status of the caliber projection of the confirmed execution
     * @param mostLikelyFruitsPerKg         central caliber estimate, only when the projection is estimated
     * @param commercialSizeGrade           commercial size grade of that caliber, only when estimated
     * @param estimatedKgPerHaAfterThinning campaign yield estimated with the load left by the thinning
     */
    record ThinningPlanDetail(
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
}

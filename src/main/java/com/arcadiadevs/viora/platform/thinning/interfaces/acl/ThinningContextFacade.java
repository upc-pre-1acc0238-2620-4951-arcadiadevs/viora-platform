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
}

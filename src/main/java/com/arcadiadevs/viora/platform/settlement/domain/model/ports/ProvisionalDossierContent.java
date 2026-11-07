package com.arcadiadevs.viora.platform.settlement.domain.model.ports;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;

import java.time.Instant;
import java.time.LocalDate;

/**
 * What a provisional dossier shows: the campaign of a plot as it stands at a cut-off instant, while it is still open.
 * <p>It is not stored, carries no verification code and certifies nothing: asking again later may show other values.
 * The sealed dossier stays the producer's, once the campaign is settled.</p>
 *
 * @param plotName                    the plot's name
 * @param variety                     its olive variety
 * @param areaHectares                its area
 * @param holderName                  the producer's name; null if unknown
 * @param organizationName            the organization that reads it; null if unknown
 * @param campaignYear                the campaign in progress
 * @param cutOffAt                    the instant the data was read
 * @param preThinningFruitsPerShoot   the load the sampling estimated
 * @param targetFruitsPerShoot        the sustainable load
 * @param overTargetPercentage        how much the load exceeds the target; null without both loads
 * @param prescribedRemovalPercentage the prescribed thinning
 * @param windowOpensOn               when the thinning window opens
 * @param windowClosesOn              when it closes
 * @param sampledTrees                trees sampled this campaign
 * @param lastSampledOn               the day of the last sampling round
 * @param thinnedOn                   when the producer registered the thinning; null until then
 * @param linkedNodes                 sensor nodes linked to the plot
 * @param daysWithReadings            days with readings since the campaign began
 */
public record ProvisionalDossierContent(String plotName, String variety, Double areaHectares, String holderName,
                                        String organizationName, CampaignYear campaignYear, Instant cutOffAt,
                                        Double preThinningFruitsPerShoot, Double targetFruitsPerShoot,
                                        Double overTargetPercentage, Double prescribedRemovalPercentage,
                                        LocalDate windowOpensOn, LocalDate windowClosesOn, int sampledTrees,
                                        LocalDate lastSampledOn, LocalDate thinnedOn, int linkedNodes,
                                        int daysWithReadings) {

    /**
     * Validates the parts every dossier needs.
     *
     * @throws IllegalArgumentException if the campaign or the cut-off is missing
     */
    public ProvisionalDossierContent {
        if (campaignYear == null || cutOffAt == null) {
            throw new IllegalArgumentException("settlement.provisional_dossier.content.invalid");
        }
    }
}

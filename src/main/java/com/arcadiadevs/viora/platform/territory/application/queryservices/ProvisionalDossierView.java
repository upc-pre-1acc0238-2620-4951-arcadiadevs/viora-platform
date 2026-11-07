package com.arcadiadevs.viora.platform.territory.application.queryservices;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Read model of the provisional dossier of a member plot, as T16 shows it before downloading the PDF.
 *
 * @param plotId                      the plot
 * @param plotName                    its name
 * @param producerName                its owner
 * @param organizationName            the organization of the license
 * @param variety                     its variety
 * @param areaHa                      its area
 * @param campaignYear                the campaign in progress
 * @param cutOffAt                    when the data was read; another request may give other values
 * @param preThinningFruitsPerShoot   the estimated load
 * @param targetFruitsPerShoot        the sustainable load
 * @param overTargetPercentage        the excess over the target
 * @param prescribedRemovalPercentage the prescribed thinning
 * @param windowOpensOn               when the thinning window opens
 * @param windowClosesOn              when it closes
 * @param sampledTrees                trees sampled
 * @param lastSampledOn               the last sampling round
 * @param thinnedOn                   when the thinning was registered; null while «Aún no»
 * @param linkedNodes                 sensor nodes linked
 * @param daysWithReadings            days with readings this campaign
 */
public record ProvisionalDossierView(String plotId, String plotName, String producerName, String organizationName,
                                     String variety, Double areaHa, int campaignYear, Instant cutOffAt,
                                     Double preThinningFruitsPerShoot, Double targetFruitsPerShoot,
                                     Double overTargetPercentage, Double prescribedRemovalPercentage,
                                     LocalDate windowOpensOn, LocalDate windowClosesOn, int sampledTrees,
                                     LocalDate lastSampledOn, LocalDate thinnedOn, int linkedNodes,
                                     int daysWithReadings) {
}

package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import java.time.Instant;
import java.time.LocalDate;

/**
 * What Settlement tells about the provisional dossier of a member plot.
 *
 * @param plotName                    the plot's name
 * @param variety                     its variety
 * @param areaHectares                its area
 * @param campaignYear                the campaign in progress
 * @param cutOffAt                    when the data was read
 * @param preThinningFruitsPerShoot   the estimated load
 * @param targetFruitsPerShoot        the sustainable load
 * @param overTargetPercentage        the excess over the target
 * @param prescribedRemovalPercentage the prescribed thinning
 * @param windowOpensOn               when the thinning window opens
 * @param windowClosesOn              when it closes
 * @param sampledTrees                trees sampled
 * @param lastSampledOn               the last sampling round
 * @param thinnedOn                   when the thinning was registered
 * @param linkedNodes                 sensor nodes linked
 * @param daysWithReadings            days with readings this campaign
 */
public record ProvisionalDossierFacts(String plotName, String variety, Double areaHectares, int campaignYear,
                                      Instant cutOffAt, Double preThinningFruitsPerShoot,
                                      Double targetFruitsPerShoot, Double overTargetPercentage,
                                      Double prescribedRemovalPercentage, LocalDate windowOpensOn,
                                      LocalDate windowClosesOn, int sampledTrees, LocalDate lastSampledOn,
                                      LocalDate thinnedOn, int linkedNodes, int daysWithReadings) {
}

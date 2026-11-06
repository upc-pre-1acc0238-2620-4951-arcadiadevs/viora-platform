package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import java.time.LocalDate;

/**
 * What Thinning tells about the campaign of a plot.
 *
 * @param issued                        whether the sampling was enough to prescribe
 * @param percentageToRemove            the fruits to remove, in percent; null until prescribed
 * @param preThinningFruitsPerShoot     the estimated load
 * @param targetFruitsPerShoot          the sustainable load
 * @param windowOpensOn                 when the thinning window opens
 * @param windowClosesOn                when it closes
 * @param sampledTrees                  how many trees were sampled
 * @param lastSampledOn                 the day of the last sampling round
 * @param executedOn                    when the producer registered the thinning; null until then
 * @param estimatedKgPerHaAfterThinning the yield estimated after thinning
 */
public record ThinningFacts(boolean issued, Double percentageToRemove, Double preThinningFruitsPerShoot,
                            Double targetFruitsPerShoot, LocalDate windowOpensOn, LocalDate windowClosesOn,
                            int sampledTrees, LocalDate lastSampledOn, LocalDate executedOn,
                            Double estimatedKgPerHaAfterThinning) {
}

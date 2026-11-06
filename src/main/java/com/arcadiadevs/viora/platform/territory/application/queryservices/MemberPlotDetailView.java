package com.arcadiadevs.viora.platform.territory.application.queryservices;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Read model of a member plot as the manager reads it, read-only: load, prescription, thinning and the informative
 * bearing index, with the phone to coordinate a visit.
 *
 * @param plotId                        the plot
 * @param plotName                      its name
 * @param producerId                    its owner
 * @param producerName                  the owner's name
 * @param producerPhone                 the owner's phone, for «Llamar» and «WhatsApp»
 * @param variety                       its olive variety
 * @param areaHa                        its area in hectares
 * @param sectorZoneName                its sector's name; null without one
 * @param sectorStatus                  {@code ASSIGNED}, {@code PENDING} or {@code NONE}
 * @param riskLevel                     its color
 * @param reasons                       why
 * @param overloadPercent               overload in percent; null without a valid sampling
 * @param sampledTrees                  trees sampled this campaign
 * @param lastSampledOn                 the day of the last sampling round
 * @param preThinningFruitsPerShoot     estimated load, fruits per shoot
 * @param targetFruitsPerShoot          sustainable load, fruits per shoot
 * @param percentageToRemove            the prescribed thinning, in percent
 * @param windowOpensOn                 when the thinning window opens
 * @param windowClosesOn                when it closes
 * @param thinningExecutedOn            when the producer registered the thinning; null until then
 * @param estimatedKgPerHaAfterThinning yield estimated after thinning
 * @param bearingIndex                  the biennial bearing index; informative, it never changes the color
 * @param bearingCategory               its category
 * @param evaluatedAt                   when this was read
 */
public record MemberPlotDetailView(String plotId, String plotName, String producerId, String producerName,
                                   String producerPhone, String variety, Double areaHa, String sectorZoneName,
                                   String sectorStatus, String riskLevel, List<RiskReasonView> reasons,
                                   Double overloadPercent, Integer sampledTrees, LocalDate lastSampledOn,
                                   Double preThinningFruitsPerShoot, Double targetFruitsPerShoot,
                                   Double percentageToRemove, LocalDate windowOpensOn, LocalDate windowClosesOn,
                                   LocalDate thinningExecutedOn, Double estimatedKgPerHaAfterThinning,
                                   Double bearingIndex, String bearingCategory, Instant evaluatedAt) {
}

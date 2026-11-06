package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A plot of a member who shares, read-only, as the manager prepares a visit.
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
@Schema(name = "MemberPlotDetailResource", description = "Plot of a member who shares, read-only")
public record MemberPlotDetailResource(
        String plotId,
        @Schema(example = "La Yarada 02") String plotName,
        String producerId,
        @Schema(example = "Teodoro Mamani") String producerName,
        @Schema(example = "+51952184337") String producerPhone,
        @Schema(example = "SEVILLANA") String variety,
        @Schema(example = "2.5") Double areaHa,
        @Schema(example = "La Yarada Baja") String sectorZoneName,
        @Schema(example = "ASSIGNED") String sectorStatus,
        @Schema(example = "CRITICAL") String riskLevel,
        List<RiskReasonResource> reasons,
        @Schema(example = "43.0") Double overloadPercent,
        @Schema(example = "5") Integer sampledTrees,
        LocalDate lastSampledOn,
        @Schema(example = "2.0") Double preThinningFruitsPerShoot,
        @Schema(example = "1.4") Double targetFruitsPerShoot,
        @Schema(example = "30.0") Double percentageToRemove,
        LocalDate windowOpensOn,
        LocalDate windowClosesOn,
        LocalDate thinningExecutedOn,
        @Schema(example = "8200.0") Double estimatedKgPerHaAfterThinning,
        @Schema(example = "0.45") Double bearingIndex,
        String bearingCategory,
        Instant evaluatedAt
) {
}

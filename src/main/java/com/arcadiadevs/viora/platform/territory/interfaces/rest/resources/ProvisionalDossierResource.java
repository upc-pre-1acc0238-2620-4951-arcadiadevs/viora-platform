package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The provisional dossier of a member plot: provisional, not certified, with the instant its data was read.
 *
 * @param documentType                always {@code PROVISIONAL}: the sealed dossier is another document
 * @param plotId                      the plot
 * @param plotName                    its name
 * @param producerName                its owner
 * @param organizationName            the organization of the license
 * @param variety                     its variety
 * @param areaHa                      its area
 * @param campaignYear                the campaign in progress
 * @param cutOffAt                    when the data was read
 * @param preThinningFruitsPerShoot   the estimated load, fruits per shoot
 * @param targetFruitsPerShoot        the sustainable load, fruits per shoot
 * @param overTargetPercentage        the excess over the target, in percent
 * @param prescribedRemovalPercentage the prescribed thinning, in percent
 * @param windowOpensOn               when the thinning window opens
 * @param windowClosesOn              when it closes
 * @param sampledTrees                trees sampled
 * @param lastSampledOn               the last sampling round
 * @param thinnedOn                   when the thinning was registered; null while «Aún no»
 * @param linkedNodes                 sensor nodes linked
 * @param daysWithReadings            days with readings this campaign
 */
@Schema(name = "ProvisionalDossierResource", description = "Provisional, not certified dossier of a member plot")
public record ProvisionalDossierResource(
        @Schema(example = "PROVISIONAL", allowableValues = "PROVISIONAL") String documentType,
        String plotId,
        @Schema(example = "La Yarada 02") String plotName,
        @Schema(example = "Teodoro Mamani") String producerName,
        @Schema(example = "Cooperativa Olivar del Sur") String organizationName,
        @Schema(example = "SEVILLANA") String variety,
        @Schema(example = "2.5") Double areaHa,
        @Schema(example = "2026") int campaignYear,
        Instant cutOffAt,
        @Schema(example = "2.0") Double preThinningFruitsPerShoot,
        @Schema(example = "1.4") Double targetFruitsPerShoot,
        @Schema(example = "42.9") Double overTargetPercentage,
        @Schema(example = "30.0") Double prescribedRemovalPercentage,
        LocalDate windowOpensOn,
        LocalDate windowClosesOn,
        @Schema(example = "5") int sampledTrees,
        LocalDate lastSampledOn,
        LocalDate thinnedOn,
        @Schema(example = "2") int linkedNodes,
        @Schema(example = "182") int daysWithReadings
) {
}

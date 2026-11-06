package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * The sector of a member plot.
 *
 * @param plotId         the plot
 * @param plotName       its name; only unique per producer
 * @param producerId     its owner
 * @param producerName   the owner's name
 * @param status         {@code ASSIGNED}, or {@code PENDING} while «Por confirmar»
 * @param sectorZoneId   the sector; null while pending
 * @param sectorZoneName the sector's current name; null while pending
 * @param proposedName   what the producer wrote; null once assigned
 * @param updatedAt      when the sector last changed
 */
@Schema(name = "PlotSectorAssignmentResource", description = "Sector of a member plot")
public record PlotSectorAssignmentResource(
        String plotId,
        @Schema(example = "Parcela 3") String plotName,
        String producerId,
        @Schema(example = "Julio Quispe") String producerName,
        @Schema(example = "PENDING") String status,
        String sectorZoneId,
        @Schema(example = "Magollo") String sectorZoneName,
        @Schema(example = "Cerro Blanco") String proposedName,
        Instant updatedAt
) {
}

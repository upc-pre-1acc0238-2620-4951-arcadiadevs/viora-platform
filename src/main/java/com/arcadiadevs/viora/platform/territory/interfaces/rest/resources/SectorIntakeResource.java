package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The intake of one sector.
 *
 * @param sectorZoneId   the sector; null for «Por confirmar» or no sector
 * @param sectorZoneName its name
 * @param sectorStatus   {@code ASSIGNED}, {@code PENDING} or {@code NONE}
 * @param plots          the member plots counted there
 * @param sampledPlots   those sampled this campaign, or those that settled a past one
 * @param tons           the projected or settled tons; null when none of its plots can tell
 */
@Schema(name = "SectorIntakeResource", description = "Intake of one sector")
public record SectorIntakeResource(
        String sectorZoneId,
        @Schema(example = "La Yarada Baja") String sectorZoneName,
        @Schema(example = "ASSIGNED") String sectorStatus,
        @Schema(example = "18") int plots,
        @Schema(example = "15") int sampledPlots,
        @Schema(example = "390.0") Double tons
) {
}

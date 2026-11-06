package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A sector of the license list.
 *
 * @param id        the sector zone
 * @param name      its name
 * @param plotCount the member plots in it; plots «Por confirmar» do not count
 */
@Schema(name = "SectorZoneResource", description = "Sector of the license list")
public record SectorZoneResource(
        String id,
        @Schema(example = "La Yarada Baja") String name,
        @Schema(example = "18") int plotCount
) {
}

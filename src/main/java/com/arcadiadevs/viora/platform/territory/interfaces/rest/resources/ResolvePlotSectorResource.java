package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request payload a manager sends to review a plot «Por confirmar».
 *
 * @param sectorZoneId the existing sector it means («Es el mismo que…»); left out, the written name is added as a
 *                     sector («Añadir como sector»)
 */
@Schema(name = "ResolvePlotSectorResource", description = "Existing sector the written name means, if any",
        example = "{\"sectorZoneId\": \"5f0c7b0e-8a3d-4c55-9a6e-1d2f3b4c5d6e\"}")
public record ResolvePlotSectorResource(
        @Schema(description = "Existing sector; leave it out to add the written name as a sector",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String sectorZoneId
) {
}

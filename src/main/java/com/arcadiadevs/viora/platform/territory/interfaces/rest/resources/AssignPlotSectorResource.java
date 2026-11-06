package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request payload a member sends to give a plot a sector: exactly one of the two fields.
 *
 * @param sectorZoneId a sector of the list
 * @param name         the sector written when the list does not have it («Mi sector no está»)
 */
@Schema(name = "AssignPlotSectorResource", description = "A sector of the list, or the name the member writes",
        example = "{\"sectorZoneId\": \"5f0c7b0e-8a3d-4c55-9a6e-1d2f3b4c5d6e\"}")
public record AssignPlotSectorResource(
        @Schema(description = "Sector of the license list", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String sectorZoneId,
        @Schema(description = "Sector written by the member when it is not in the list; it matches an existing one "
                + "ignoring accents and case, otherwise it waits for the manager", example = "Cerro Blanco",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String name
) {
}

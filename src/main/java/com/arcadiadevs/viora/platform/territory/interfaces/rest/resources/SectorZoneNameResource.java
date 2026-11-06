package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload with the name of a sector, to add it or to rename it.
 *
 * @param name the name; accents and case do not make it a different sector
 */
@Schema(name = "SectorZoneNameResource", description = "Name of a sector to add or rename",
        example = "{\"name\": \"Cerro Blanco\"}")
public record SectorZoneNameResource(
        @NotBlank(message = "Name cannot be blank")
        @Schema(description = "Name of the sector, 2 to 60 characters", example = "Cerro Blanco")
        String name
) {
}

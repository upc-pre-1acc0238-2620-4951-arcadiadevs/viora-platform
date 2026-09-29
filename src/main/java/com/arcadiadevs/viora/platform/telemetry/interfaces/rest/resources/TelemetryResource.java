package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Representation resource for an hourly agroclimatic telemetry observation.
 *
 * @param id                 unique observation identifier
 * @param plotId             referenced orchard plot UUID
 * @param temperature        ambient air temperature in °C
 * @param humidity           relative air humidity in %
 * @param soilMoisture       volumetric soil moisture in %
 * @param solarRadiation     solar irradiance in W/m²
 * @param stemWaterPotential stem water potential in MPa (optional)
 * @param recordedAt         UTC timestamp when the reading was recorded
 */
@Schema(description = "Represents an hourly agroclimatic and soil telemetry observation.")
public record TelemetryResource(
        @Schema(description = "Unique observation identifier UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Referenced orchard plot UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID plotId,

        @Schema(description = "Ambient air temperature in degrees Celsius (°C)", example = "23.5")
        Double temperature,

        @Schema(description = "Relative air humidity percentage (%)", example = "58.2")
        Double humidity,

        @Schema(description = "Volumetric soil moisture percentage (%)", example = "28.4")
        Double soilMoisture,

        @Schema(description = "Global horizontal solar radiation in W/m²", example = "680.0")
        Double solarRadiation,

        @Schema(description = "Stem water potential in MPa (nullable)", example = "-1.2")
        Double stemWaterPotential,

        @Schema(description = "UTC timestamp when observation was recorded", example = "2026-09-28T14:00:00Z")
        Instant recordedAt
) {
}

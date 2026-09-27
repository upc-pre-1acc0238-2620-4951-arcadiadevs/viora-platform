package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Public REST response resource representing an IoT virtual sensor node.
 *
 * @param id                    the unique device identifier UUID
 * @param plotId                the logical reference UUID of the instrumented plot
 * @param name                  the descriptive device name
 * @param type                  the functional classification (MICROCLIMATE or SOIL_PROBE)
 * @param depthCm               the installation depth in centimeters
 * @param soilTextureType       the soil textural classification
 * @param calibrationMultiplier the empirical adjustment multiplier
 * @param status                the operational lifecycle status
 * @param lastReadingTimestamp  the timestamp of the most recent reading, if any
 * @param revision              the optimistic concurrency revision number
 */
@Schema(
        name = "IoTDeviceResource",
        description = "Response resource representing an IoT sensor node",
        example = "{\"id\": \"550e8400-e29b-41d4-a716-446655440001\", \"plotId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"name\": \"Sonda Edafica Sector Norte\", \"type\": \"SOIL_PROBE\", \"depthCm\": 30, \"soilTextureType\": \"SANDY_LOAM\", \"calibrationMultiplier\": 1.0, \"status\": \"ACTIVE\", \"lastReadingTimestamp\": null, \"revision\": 0}"
)
@NullMarked
public record IoTDeviceResource(
        @Schema(description = "Unique device identifier", example = "550e8400-e29b-41d4-a716-446655440001")
        String id,

        @Schema(description = "Managing plot identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String plotId,

        @Schema(description = "Descriptive name of the device", example = "Sonda Edafica Sector Norte")
        String name,

        @Schema(description = "Functional device classification", example = "SOIL_PROBE", allowableValues = {"MICROCLIMATE", "SOIL_PROBE"})
        String type,

        @Schema(description = "Installation depth in centimeters", example = "30")
        @Nullable Integer depthCm,

        @Schema(description = "Soil textural type", example = "SANDY_LOAM")
        String soilTextureType,

        @Schema(description = "Empirical calibration multiplier", example = "1.0")
        Double calibrationMultiplier,

        @Schema(description = "Operational status of the device", example = "ACTIVE", allowableValues = {"ACTIVE", "PAUSED", "UNLINKED"})
        String status,

        @Schema(description = "Timestamp of the most recent telemetry reading", example = "2026-09-26T12:00:00Z")
        @Nullable Instant lastReadingTimestamp,

        @Schema(description = "Optimistic locking revision number", example = "0")
        Long revision
) {
}

package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Representation model representing the payload to calibrate an IoT device's calculation factors.
 *
 * @param calibrationMultiplier the empirical adjustment factor between 0.50 and 2.00
 * @param soilTextureType       the soil textural classification string (optional, for soil probes)
 * @param depthCm               the installation depth in centimeters (optional, for soil probes)
 */
@Schema(
        name = "CalibrateIoTDeviceResource",
        description = "Request payload for updating device calibration multiplier, soil texture, and depth",
        example = "{\"calibrationMultiplier\": 1.15, \"soilTextureType\": \"LOAM\", \"depthCm\": 45}"
)
public record CalibrateIoTDeviceResource(
        @NotNull(message = "device.calibration.null")
        @DecimalMin(value = "0.50", message = "device.calibration.out_of_range")
        @DecimalMax(value = "2.00", message = "device.calibration.out_of_range")
        @Schema(description = "Empirical adjustment multiplier (0.50 - 2.00)", example = "1.15", requiredMode = Schema.RequiredMode.REQUIRED)
        Double calibrationMultiplier,

        @Schema(description = "Soil textural type (e.g. SANDY, LOAM, CLAY)", example = "LOAM")
        String soilTextureType,

        @Positive(message = "device.depth.invalid")
        @Schema(description = "Sensor installation depth in centimeters", example = "45")
        Integer depthCm
) {
}

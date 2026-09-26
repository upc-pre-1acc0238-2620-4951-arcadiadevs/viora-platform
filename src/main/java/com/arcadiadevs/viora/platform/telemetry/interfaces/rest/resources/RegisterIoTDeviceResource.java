package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Request payload resource for registering a new IoT virtual sensor node bound to an orchard plot.
 *
 * @param name                  the descriptive name of the device
 * @param type                  the functional classification (MICROCLIMATE or SOIL_PROBE)
 * @param depthCm               the installation depth in centimeters (required for soil probes)
 * @param soilTextureType       the soil textural classification string
 * @param calibrationMultiplier the empirical adjustment multiplier
 */
@Schema(
        name = "RegisterIoTDeviceResource",
        description = "Request payload for registering and binding an IoT device to a plot",
        example = "{\"name\": \"Sonda Edafica Sector Norte\", \"type\": \"SOIL_PROBE\", \"depthCm\": 30, \"soilTextureType\": \"SANDY_LOAM\", \"calibrationMultiplier\": 1.0}"
)
@NullMarked
public record RegisterIoTDeviceResource(
        @NotBlank(message = "Device name cannot be blank")
        @Size(min = 2, max = 100, message = "Device name must be between 2 and 100 characters")
        @Schema(description = "Descriptive name of the sensor device", example = "Sonda Edafica Sector Norte", minLength = 2, maxLength = 100)
        String name,

        @NotBlank(message = "Device type cannot be blank")
        @Schema(description = "Functional device classification", example = "SOIL_PROBE", allowableValues = {"MICROCLIMATE", "SOIL_PROBE"})
        String type,

        @Min(value = 1, message = "Sensor depth must be at least 1 cm")
        @Max(value = 200, message = "Sensor depth must not exceed 200 cm")
        @Schema(description = "Installation depth in centimeters for soil probes", example = "30")
        @Nullable Integer depthCm,

        @Schema(description = "Soil textural type", example = "SANDY_LOAM", allowableValues = {
                "SANDY", "LOAMY_SAND", "SANDY_LOAM", "LOAM", "SILT_LOAM", "SILT",
                "SANDY_CLAY_LOAM", "CLAY_LOAM", "SILTY_CLAY_LOAM", "SANDY_CLAY", "SILTY_CLAY", "CLAY", "NOT_APPLICABLE"
        })
        @Nullable String soilTextureType,

        @DecimalMin(value = "0.50", message = "Calibration multiplier must be at least 0.50")
        @DecimalMax(value = "2.00", message = "Calibration multiplier must not exceed 2.00")
        @Schema(description = "Empirical calibration multiplier in range [0.50, 2.00]", example = "1.0")
        @Nullable Double calibrationMultiplier
) {
}

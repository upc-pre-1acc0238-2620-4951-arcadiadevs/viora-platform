package com.arcadiadevs.viora.platform.phenology.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Request payload resource for rectifying an existing annual olive harvest yield.
 *
 * @param totalYieldKg the rectified total volume of olives harvested in kilograms
 * @param greenKg      the weight in kilograms of green table olives
 * @param blackKg      the weight in kilograms of black natural olives
 * @param notes        agronomic remarks or weighbridge adjustment notes
 */
@Schema(
        name = "RectifyHarvestYieldResource",
        description = "Request payload for rectifying an annual plot harvest yield",
        example = "{\"totalYieldKg\": 15800.0, \"greenKg\": 9100.0, \"blackKg\": 6700.0, \"notes\": \"Audited calibration adjustment from mill weighbridge.\"}"
)
@NullMarked
public record RectifyHarvestYieldResource(
        @NotNull(message = "phenology.harvest_yield.null")
        @DecimalMin(value = "0.01", message = "phenology.harvest_yield.total_positive")
        @Schema(description = "Total olive fruit mass harvested in kilograms", example = "15800.0", requiredMode = Schema.RequiredMode.REQUIRED)
        Double totalYieldKg,

        @DecimalMin(value = "0.0", message = "phenology.harvest_yield.green_negative")
        @Schema(description = "Green olive mass in kilograms for table preservation", example = "9100.0")
        @Nullable Double greenKg,

        @DecimalMin(value = "0.0", message = "phenology.harvest_yield.black_negative")
        @Schema(description = "Black olive mass in kilograms", example = "6700.0")
        @Nullable Double blackKg,

        @Size(max = 500, message = "validation.field.prefix")
        @Schema(description = "Optional agronomic comments or weighbridge slips", example = "Audited calibration adjustment from mill weighbridge.")
        @Nullable String notes
) {
}

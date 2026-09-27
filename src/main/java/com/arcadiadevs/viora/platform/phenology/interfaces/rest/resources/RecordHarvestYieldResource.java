package com.arcadiadevs.viora.platform.phenology.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Request payload resource for recording an annual olive harvest yield.
 *
 * @param campaignYear the agricultural campaign year
 * @param totalYieldKg the total volume of olives harvested in kilograms
 * @param greenKg      the weight in kilograms of green table olives
 * @param blackKg      the weight in kilograms of black natural olives
 * @param notes        agronomic remarks or batch notes
 */
@Schema(
        name = "RecordHarvestYieldResource",
        description = "Request payload for recording an annual plot harvest yield",
        example = "{\"campaignYear\": 2025, \"totalYieldKg\": 14250.0, \"greenKg\": 8200.0, \"blackKg\": 6050.0, \"notes\": \"Optimal harvest\"}"
)
@NullMarked
public record RecordHarvestYieldResource(
        @NotNull(message = "phenology.campaign_year.null")
        @Min(value = 1980, message = "phenology.campaign_year.invalid")
        @Max(value = 2100, message = "phenology.campaign_year.invalid")
        @Schema(description = "Agricultural campaign year (1980 - 2100)", example = "2025", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer campaignYear,

        @NotNull(message = "phenology.harvest_yield.null")
        @DecimalMin(value = "0.01", message = "phenology.harvest_yield.total_positive")
        @Schema(description = "Total olive fruit mass harvested in kilograms", example = "14250.0", requiredMode = Schema.RequiredMode.REQUIRED)
        Double totalYieldKg,

        @DecimalMin(value = "0.0", message = "phenology.harvest_yield.green_negative")
        @Schema(description = "Green olive mass in kilograms for table preservation", example = "8200.0")
        @Nullable Double greenKg,

        @DecimalMin(value = "0.0", message = "phenology.harvest_yield.black_negative")
        @Schema(description = "Black olive mass in kilograms", example = "6050.0")
        @Nullable Double blackKg,

        @Size(max = 500, message = "validation.field.prefix")
        @Schema(description = "Optional agronomic comments or weighbridge slips", example = "Optimal harvest")
        @Nullable String notes
) {
}

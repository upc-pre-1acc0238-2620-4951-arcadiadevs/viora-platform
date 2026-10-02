package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** Formal weighing of the delivered harvest of a campaign. */
@Schema(description = "Harvest settlement input; weights in kilograms, at least one of them positive")
public record SettleHarvestResource(
        @NotNull(message = "{settlement.campaign_year.invalid}")
        @Min(value = 2000, message = "{settlement.campaign_year.invalid}")
        @Max(value = 2100, message = "{settlement.campaign_year.invalid}")
        @Schema(description = "Settled campaign, 2000 to 2100", example = "2026") Integer campaignYear,
        @NotNull(message = "{settlement.weight.invalid}") @PositiveOrZero(message = "{settlement.weight.invalid}")
        @Schema(description = "Green olives delivered, kg", example = "8200.0") Double greenOlivesKg,
        @NotNull(message = "{settlement.weight.invalid}") @PositiveOrZero(message = "{settlement.weight.invalid}")
        @Schema(description = "Black olives delivered, kg", example = "6050.0") Double blackOlivesKg,
        @Positive(message = "{settlement.fruits_per_kg.invalid}")
        @Schema(description = "Optional commercial caliber of the delivered table olives, fruits per kilogram "
                + "(IOC size scale). It calibrates Viora's caliber projection.", example = "105.0", nullable = true)
        Double commercialFruitsPerKg,
        @Size(max = 1000, message = "{settlement.notes.too_long}")
        @Schema(description = "Optional notes, up to 1000 characters", example = "Campaign weights verified.")
        String notes) {
}

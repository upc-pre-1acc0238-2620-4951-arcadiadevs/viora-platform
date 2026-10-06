package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

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
        String notes,
        @NotNull(message = "{settlement.weighed_on.null}")
        @Schema(description = "Date the delivered olives were weighed. It cannot be in the future.",
                example = "2026-11-18") LocalDate weighedOn,
        // No @Size here: it would count the surrounding spaces. MillTicketNumber trims first and then enforces the
        // 30 characters, so a value that fits once trimmed is accepted.
        @Schema(description = "Optional ticket number of the mill that received the olives, up to 30 characters "
                + "once trimmed. It is the weighbridge ticket the mill prints on delivery.", example = "MT-88213",
                maxLength = 30, nullable = true)
        String millTicketNumber) {
}

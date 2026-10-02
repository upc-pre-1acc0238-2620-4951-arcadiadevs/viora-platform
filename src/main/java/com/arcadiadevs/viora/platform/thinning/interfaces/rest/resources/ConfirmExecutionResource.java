package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Field execution input. Dates use executedDate as specified by the endpoint contract. */
@Schema(description = "Actual thinning labor performed in the field")
public record ConfirmExecutionResource(
        @NotNull(message = "{thinning.execution.date.invalid}")
        @Schema(description = "Execution date; cannot be in the future (UTC)", example = "2026-09-28") LocalDate executedDate,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0")
        @Schema(description = "Actual removal percentage in [0, 100]", example = "25.0") Double actualRemovalPercentage,
        @NotNull @DecimalMin("0.0")
        @Schema(description = "Removed biomass in kilograms", example = "420.0") Double removedKg,
        @NotNull @Positive
        @Schema(description = "Number of workers", example = "4") Integer laborCrewSize,
        @Size(max = 2000, message = "{thinning.execution.notes.too_long}")
        @Schema(description = "Optional field notes, up to 2000 characters") String notes) {
}

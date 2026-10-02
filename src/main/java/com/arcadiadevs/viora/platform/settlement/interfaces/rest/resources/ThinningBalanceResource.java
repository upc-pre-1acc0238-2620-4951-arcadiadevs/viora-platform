package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/** Balance of the campaign against its thinning prescription, frozen at settlement. */
@Schema(description = "Compares only prescribed and actual percentage of fruits removed. NOT_RECORDED is not a "
        + "non-compliance: a balanced plot may need no thinning.")
public record ThinningBalanceResource(
        @Schema(allowableValues = {"EXECUTED_ON_TIME", "EXECUTED_LATE", "NOT_RECORDED"}) String status,
        @Schema(nullable = true) LocalDate executedDate,
        @Schema(description = "Percentage the prescription asked to remove", nullable = true)
        Double prescribedRemovalPercentage,
        @Schema(description = "Percentage actually removed", nullable = true) Double actualRemovalPercentage,
        @Schema(description = "Actual minus prescribed, in percentage points", nullable = true)
        Double deviationPercentagePoints) {
}

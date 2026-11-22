package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Fruits weighed during a sampling round, to estimate their caliber.
 *
 * @param fruitCount  the fruits weighed
 * @param weightGrams what they weigh, in grams
 */
@Schema(name = "FruitSampleResource", description = "Fruits weighed during the round, to estimate their caliber")
public record FruitSampleResource(
        @NotNull(message = "thinning.fruit_sample.count.invalid")
        @Min(value = 1, message = "thinning.fruit_sample.count.invalid")
        @Max(value = 1000, message = "thinning.fruit_sample.count.invalid")
        @Schema(description = "Fruits weighed (1-1000)", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer fruitCount,

        @NotNull(message = "thinning.fruit_sample.weight.invalid")
        @DecimalMin(value = "0", inclusive = false, message = "thinning.fruit_sample.weight.invalid")
        @DecimalMax(value = "10000", message = "thinning.fruit_sample.weight.invalid")
        @Schema(description = "Weight of those fruits in grams (> 0)", example = "250.0",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Double weightGrams
) {
}

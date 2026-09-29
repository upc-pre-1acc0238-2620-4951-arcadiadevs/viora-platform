package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;

/**
 * REST resource item representing an individual tree sampling observation within a batch.
 *
 * @param treeTag         physical tag identifier of the tree
 * @param shootCount      number of evaluated shoots (must be > 0)
 * @param fruitSetCount   number of counted fruits (must be >= 0)
 * @param trunkDiameterMm trunk diameter in millimeters (must be > 0)
 * @param samplingDate    date when sample was taken (cannot be future date)
 */
@Schema(
        name = "TreeSampleResourceItem",
        description = "Individual tree inspection data point within a sampling round",
        example = "{\"treeTag\": \"T-01\", \"shootCount\": 10, \"fruitSetCount\": 120, \"trunkDiameterMm\": 165.5, \"samplingDate\": \"2026-09-28\"}"
)
@NullMarked
public record TreeSampleResourceItem(
        @NotBlank(message = "thinning.tree_tag.null_or_empty")
        @Schema(description = "Physical tag identifier of the sampled olive tree", example = "T-01", requiredMode = Schema.RequiredMode.REQUIRED)
        String treeTag,

        @NotNull(message = "thinning.shoot_count.positive")
        @Min(value = 1, message = "thinning.shoot_count.positive")
        @Schema(description = "Total number of counted representative shoots (> 0)", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer shootCount,

        @NotNull(message = "thinning.fruit_count.negative")
        @Min(value = 0, message = "thinning.fruit_count.negative")
        @Schema(description = "Total number of set fruits observed across shoots (>= 0)", example = "120", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer fruitSetCount,

        @NotNull(message = "thinning.trunk_diameter.positive")
        @DecimalMin(value = "0.01", message = "thinning.trunk_diameter.positive")
        @Schema(description = "Trunk diameter measured in millimeters (> 0)", example = "165.5", requiredMode = Schema.RequiredMode.REQUIRED)
        Double trunkDiameterMm,

        @NotNull(message = "thinning.sampling_date.null")
        @PastOrPresent(message = "thinning.sampling_date.future")
        @Schema(description = "Date when tree sampling was conducted in field (cannot be future)", example = "2026-09-28", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate samplingDate
) {
}

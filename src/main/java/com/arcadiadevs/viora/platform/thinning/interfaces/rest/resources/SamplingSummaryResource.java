package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

/**
 * REST response resource representing the statistical sampling summary of an olive plot and campaign.
 *
 * @param plotId             the plot UUID
 * @param campaignYear       the agricultural campaign year
 * @param sampledTreesCount    total unique evaluated trees
 * @param sampledShootsCount   total shoots counted
 * @param sampledFruitSetCount total number of set fruits observed across all sampled shoots
 * @param meanFruitsPerShoot   average fruits per sampled shoot
 * @param isRepresentative     whether minimum statistical confidence was reached (minimum 5 trees)
 * @param treesNeeded          count of additional trees needed to achieve representativeness
 * @param loadUnit             unit of the load: {@code FRUITS_PER_SHOOT}
 */
@Schema(
        name = "SamplingSummaryResource",
        description = "Response resource representing the statistical representativeness and shoot density of field samplings",
        example = "{\"plotId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"campaignYear\": 2026, \"sampledTreesCount\": 5, \"sampledShootsCount\": 60, \"sampledFruitSetCount\": 123, \"meanFruitsPerShoot\": 0.903, \"isRepresentative\": true, \"treesNeeded\": 0, \"loadUnit\": \"FRUITS_PER_SHOOT\"}"
)
@NullMarked
public record SamplingSummaryResource(
        @Schema(description = "Unique plot identifier UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String plotId,

        @Schema(description = "Agricultural campaign year", example = "2026")
        Integer campaignYear,

        @Schema(description = "Total number of unique trees evaluated in the campaign", example = "5")
        Integer sampledTreesCount,

        @Schema(description = "Total number of shoots observed across all sampling rounds", example = "60")
        Integer sampledShootsCount,

        @Schema(description = "Total number of set fruits observed across all sampled shoots", example = "123")
        Integer sampledFruitSetCount,

        @Schema(description = "Assessed mean fruits per sampled shoot", example = "0.903")
        Double meanFruitsPerShoot,

        @Schema(description = "Whether the sample fulfills the statistical threshold of at least 5 evaluated trees", example = "true")
        Boolean isRepresentative,

        @Schema(description = "Number of additional trees required to achieve statistical confidence", example = "0")
        Integer treesNeeded,

        @Schema(description = "Unit of the mean load", example = "FRUITS_PER_SHOOT")
        String loadUnit
) {
}

package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

/**
 * REST response resource representing the statistical sampling summary of an olive plot and campaign.
 *
 * @param plotId             the plot UUID
 * @param campaignYear       the agricultural campaign year
 * @param sampledTreesCount  total unique evaluated trees
 * @param sampledShootsCount total shoots counted
 * @param meanFruitsPerMeter average fruit load density per canopy meter
 * @param isRepresentative   whether minimum statistical confidence was reached (minimum 5 trees)
 * @param treesNeeded        count of additional trees needed to achieve representativeness
 */
@Schema(
        name = "SamplingSummaryResource",
        description = "Response resource representing the statistical representativeness and shoot density of field samplings",
        example = "{\"plotId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"campaignYear\": 2026, \"sampledTreesCount\": 5, \"sampledShootsCount\": 60, \"meanFruitsPerMeter\": 54.2, \"isRepresentative\": true, \"treesNeeded\": 0}"
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

        @Schema(description = "Assessed mean fruit set density per linear canopy meter", example = "54.2")
        Double meanFruitsPerMeter,

        @Schema(description = "Whether the sample fulfills the statistical threshold of at least 5 evaluated trees", example = "true")
        Boolean isRepresentative,

        @Schema(description = "Number of additional trees required to achieve statistical confidence", example = "0")
        Integer treesNeeded
) {
}

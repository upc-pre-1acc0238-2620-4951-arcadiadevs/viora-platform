package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

/**
 * REST response resource representing the field sampling progress and coverage state of an olive plot.
 *
 * @param plotId            plot identifier UUID
 * @param plotName          human-readable plot name
 * @param variety           botanical olive variety
 * @param areaHectares      plot surface area in hectares
 * @param campaignYear      agricultural campaign year
 * @param samplingStatus    sampling status (NOT_STARTED, IN_PROGRESS, COMPLETED)
 * @param sampledTreesCount unique evaluated trees count
 * @param treesNeeded       additional trees needed to achieve representativeness
 * @param isRepresentative  whether minimum representativeness threshold is satisfied
 */
@Schema(
        name = "PlotSamplingStateResource",
        description = "Sampling coverage and progress state for a specific plot and campaign"
)
@NullMarked
public record PlotSamplingStateResource(
        @Schema(description = "Unique plot identifier UUID", example = "550e8400-e29b-41d4-a716-446655440001")
        String plotId,

        @Schema(description = "Human-readable plot name", example = "Lote Norte")
        String plotName,

        @Schema(description = "Olive variety", example = "Sevillana")
        String variety,

        @Schema(description = "Surface area in hectares", example = "1.5")
        Double areaHectares,

        @Schema(description = "Agricultural campaign year", example = "2026")
        Integer campaignYear,

        @Schema(description = "Current sampling status", example = "IN_PROGRESS", allowableValues = {"NOT_STARTED", "IN_PROGRESS", "COMPLETED"})
        String samplingStatus,

        @Schema(description = "Total unique trees evaluated", example = "3")
        Integer sampledTreesCount,

        @Schema(description = "Additional trees needed to reach representativeness", example = "2")
        Integer treesNeeded,

        @Schema(description = "Whether the sample satisfies statistical representativeness", example = "false")
        Boolean isRepresentative
) {
}

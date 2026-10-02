package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;

/**
 * REST response resource representing one sampled-tree observation.
 *
 * @param roundId         sampling round identifier
 * @param treeTag         physical tree identifier
 * @param shootCount      evaluated shoot count
 * @param fruitSetCount   observed set-fruit count
 * @param trunkDiameterMm trunk diameter in millimeters
 * @param samplingDate    field sampling date
 */
@Schema(name = "SamplingTreeResource", description = "Detailed tree observation within a field sampling round")
@NullMarked
public record SamplingTreeResource(
        @Schema(description = "Sampling round identifier", example = "9f44b9db-9b84-4d41-bf75-fd9f3f0be0d1")
        String roundId,

        @Schema(description = "Physical tree tag", example = "T-01")
        String treeTag,

        @Schema(description = "Number of evaluated shoots", example = "10")
        Integer shootCount,

        @Schema(description = "Number of set fruits", example = "120")
        Integer fruitSetCount,

        @Schema(description = "Trunk diameter in millimeters", example = "165.5")
        Double trunkDiameterMm,

        @Schema(description = "Date when the observation was recorded", example = "2026-09-28")
        LocalDate samplingDate
) {
}

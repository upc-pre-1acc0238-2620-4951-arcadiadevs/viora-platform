package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * REST response resource representing summary and detailed tree observations.
 *
 * @param plotId             target plot identifier
 * @param campaignYear       agricultural campaign year
 * @param sampledTreesCount  unique evaluated trees
 * @param sampledShootsCount total evaluated shoots
 * @param meanFruitsPerShoot mean fruits per sampled shoot
 * @param isRepresentative   statistical representativeness flag
 * @param treesNeeded        additional unique trees needed for representativeness
 * @param trees              ordered per-observation tree data
 */
@Schema(
        name = "SamplingDetailedResource",
        description = "Detailed field sampling view including the statistical summary and individual tree observations"
)
@NullMarked
public record SamplingDetailedResource(
        String plotId,
        Integer campaignYear,
        Integer sampledTreesCount,
        Integer sampledShootsCount,
        Double meanFruitsPerShoot,
        Boolean isRepresentative,
        Integer treesNeeded,
        @ArraySchema(schema = @Schema(implementation = SamplingTreeResource.class))
        List<SamplingTreeResource> trees
) {
}

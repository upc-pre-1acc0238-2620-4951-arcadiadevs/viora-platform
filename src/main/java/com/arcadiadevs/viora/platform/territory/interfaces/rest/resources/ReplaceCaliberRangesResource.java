package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The caliber ranges that replace the ones in force, from the biggest fruit to the smallest.
 *
 * @param ranges consecutive ranges with no overlap and no gap; empty clears them
 */
@Schema(name = "ReplaceCaliberRangesResource", description = "Caliber ranges that replace the ones in force")
public record ReplaceCaliberRangesResource(
        @NotNull(message = "territory.caliber_range_set.ranges.null")
        @Size(max = 20, message = "territory.caliber_range_set.too_many")
        @ArraySchema(schema = @Schema(implementation = CaliberRangeResource.class),
                arraySchema = @Schema(description = "From the biggest fruit to the smallest, one right after the "
                        + "other (e.g. 140-180, 181-220, 221-260); empty clears them"))
        List<@Valid CaliberRangeResource> ranges
) {
}

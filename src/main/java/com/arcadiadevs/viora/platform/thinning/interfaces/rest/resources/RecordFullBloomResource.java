package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request body to record the full bloom date observed on a plot.
 *
 * @param campaignYear campaign the bloom belongs to; defaults to the year of {@code observedOn}
 * @param observedOn   day the plot reached full bloom (most of its flowers open)
 */
@Schema(name = "RecordFullBloomResource", description = "Full bloom date observed on a plot in a campaign")
public record RecordFullBloomResource(
        @Schema(description = "Campaign the bloom belongs to; defaults to the year of observedOn", example = "2026")
        Integer campaignYear,

        @NotNull
        @Schema(description = "Day the plot reached full bloom", example = "2026-10-15")
        LocalDate observedOn
) {
}

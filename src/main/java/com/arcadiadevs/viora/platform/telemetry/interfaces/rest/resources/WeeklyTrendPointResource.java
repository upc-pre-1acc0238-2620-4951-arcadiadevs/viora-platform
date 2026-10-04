package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

import java.time.Instant;

/**
 * REST presentation resource for a trend observation data point.
 */
@Schema(name = "WeeklyTrendPointResource", description = "Single data point for weekly trend graph")
@NullMarked
public record WeeklyTrendPointResource(
        @Schema(description = "Observation timestamp", example = "2026-02-18T08:00:00Z")
        Instant timestamp,

        @Schema(description = "Recorded or forecasted observation value", example = "36.5")
        Double value,

        @Schema(description = "Threshold boundary value for contextual comparison", example = "36.0")
        Double threshold
) {
}

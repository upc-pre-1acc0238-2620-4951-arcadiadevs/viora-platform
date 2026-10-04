package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

/**
 * Summary counts of incidents grouped by status and severity.
 */
@Schema(name = "SummaryCountsResource", description = "Aggregated count summary of incidents")
@NullMarked
public record SummaryCountsResource(
        @Schema(description = "Count of active/snoozed incidents", example = "2")
        long activeCount,

        @Schema(description = "Count of active critical incidents", example = "1")
        long criticalCount,

        @Schema(description = "Count of active warning incidents", example = "1")
        long warningCount,

        @Schema(description = "Count of normalized/resolved incidents", example = "5")
        long normalizedCount
) {
}

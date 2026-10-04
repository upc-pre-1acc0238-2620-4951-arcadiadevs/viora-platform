package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * REST response resource encapsulating status count summary and incident items.
 */
@Schema(name = "AgroclimaticIncidentsSummaryResource", description = "Collection of incidents with statistical summary counters")
@NullMarked
public record AgroclimaticIncidentsSummaryResource(
        @Schema(description = "Aggregated count summary")
        SummaryCountsResource summary,

        @Schema(description = "List of agroclimatic incidents matching query filters")
        List<AgroclimaticIncidentResource> incidents
) {
    public AgroclimaticIncidentsSummaryResource {
        incidents = (incidents != null) ? List.copyOf(incidents) : List.of();
    }
}

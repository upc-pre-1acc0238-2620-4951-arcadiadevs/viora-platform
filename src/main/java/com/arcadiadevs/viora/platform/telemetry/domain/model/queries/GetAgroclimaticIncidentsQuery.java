package com.arcadiadevs.viora.platform.telemetry.domain.model.queries;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentStatus;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;

/**
 * Domain query for retrieving a list of agroclimatic incidents with optional filters.
 *
 * @param plotId   optional plot identifier filter
 * @param status   optional operational status filter
 * @param severity optional severity level filter
 */
public record GetAgroclimaticIncidentsQuery(
        PlotId plotId,
        IncidentStatus status,
        IncidentSeverity severity
) {

    /**
     * Convenience factory constructor from optional raw strings.
     *
     * @param rawPlotId   optional raw plot UUID string
     * @param rawStatus   optional raw status name
     * @param rawSeverity optional raw severity name
     */
    public GetAgroclimaticIncidentsQuery(String rawPlotId, String rawStatus, String rawSeverity) {
        this(
                (rawPlotId != null && !rawPlotId.isBlank()) ? new PlotId(rawPlotId) : null,
                (rawStatus != null && !rawStatus.isBlank()) ? IncidentStatus.from(rawStatus) : null,
                (rawSeverity != null && !rawSeverity.isBlank()) ? IncidentSeverity.from(rawSeverity) : null
        );
    }
}

package com.arcadiadevs.viora.platform.telemetry.domain.model.queries;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentStatus;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;

/**
 * Domain query for retrieving the agroclimatic incidents of a producer's plots, with optional filters.
 *
 * @param producerId the producer asking; only incidents of their plots are returned
 * @param plotId     optional plot identifier filter
 * @param status     optional operational status filter
 * @param severity   optional severity level filter
 */
public record GetAgroclimaticIncidentsQuery(
        String producerId,
        PlotId plotId,
        IncidentStatus status,
        IncidentSeverity severity
) {

    /**
     * Compact constructor enforcing the producer.
     */
    public GetAgroclimaticIncidentsQuery {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("incident.producer_id.null_or_empty");
        }
    }

    /**
     * Convenience factory constructor from optional raw strings.
     *
     * @param producerId  the producer asking
     * @param rawPlotId   optional raw plot UUID string
     * @param rawStatus   optional raw status name
     * @param rawSeverity optional raw severity name
     */
    public GetAgroclimaticIncidentsQuery(String producerId, String rawPlotId, String rawStatus, String rawSeverity) {
        this(
                producerId,
                (rawPlotId != null && !rawPlotId.isBlank()) ? new PlotId(rawPlotId) : null,
                (rawStatus != null && !rawStatus.isBlank()) ? IncidentStatus.from(rawStatus) : null,
                (rawSeverity != null && !rawSeverity.isBlank()) ? IncidentSeverity.from(rawSeverity) : null
        );
    }
}

package com.arcadiadevs.viora.platform.telemetry.domain.model.queries;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;

import java.time.Instant;

/**
 * Domain query message requesting historical telemetry series for an orchard plot within an optional date range.
 *
 * @param plotId    the monitored plot identifier
 * @param startDate the optional inclusive start instant
 * @param endDate   the optional inclusive end instant
 */
public record GetTelemetrySeriesByPlotIdQuery(
        PlotId plotId,
        Instant startDate,
        Instant endDate
) {
    /**
     * Compact constructor validating that the target plotId is non-null.
     */
    public GetTelemetrySeriesByPlotIdQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("device.plot_id.null_or_empty");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("telemetry.date_range.invalid");
        }
    }
}

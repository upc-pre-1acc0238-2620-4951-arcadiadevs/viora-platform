package com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;

import java.util.List;

/**
 * Immutable snapshot representing the full transactional state of a {@link TelemetrySeries} aggregate root.
 *
 * @param id            the unique telemetry series identifier
 * @param plotId        the reference to the monitored plot
 * @param sensorNodeId  the optional reference to the physical/virtual IoT device
 * @param currentStatus the current operational agroclimatic status
 * @param readings      the chronological list of hourly reading snapshots
 * @param revision      the optimistic concurrency version number
 */
public record TelemetrySeriesSnapshot(
        TelemetrySeriesId id,
        PlotId plotId,
        DeviceId sensorNodeId,
        TelemetrySeriesStatus currentStatus,
        List<HourlyTelemetryReadingSnapshot> readings,
        Long revision
) {
    /**
     * Compact constructor guaranteeing immutability of the readings collection and non-null components.
     */
    public TelemetrySeriesSnapshot {
        if (id == null) {
            throw new IllegalArgumentException("telemetry.series.id.null_or_empty");
        }
        if (plotId == null) {
            throw new IllegalArgumentException("device.plot_id.null_or_empty");
        }
        if (currentStatus == null) {
            throw new IllegalArgumentException("telemetry.series.status.null");
        }
        readings = readings != null ? List.copyOf(readings) : List.of();
    }
}

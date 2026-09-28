package com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure Domain Aggregate Root representing a continuous telemetry time-series for an olive orchard plot.
 *
 * <p>Enforces transactional boundaries for time-series readings, agroclimatic threshold evaluation,
 * and maintains history integrity without setters or ORM annotations.</p>
 */
public class TelemetrySeries {

    private final TelemetrySeriesId id;
    private final PlotId plotId;
    private final DeviceId sensorNodeId;
    private TelemetrySeriesStatus currentStatus;
    private final List<HourlyTelemetryReading> readings;
    private Long revision;

    private TelemetrySeries(
            TelemetrySeriesId id,
            PlotId plotId,
            DeviceId sensorNodeId,
            TelemetrySeriesStatus currentStatus,
            List<HourlyTelemetryReading> readings,
            Long revision
    ) {
        this.id = id;
        this.plotId = plotId;
        this.sensorNodeId = sensorNodeId;
        this.currentStatus = currentStatus;
        this.readings = new ArrayList<>(readings != null ? readings : List.of());
        this.revision = revision;
    }

    /**
     * Domain factory creating a new empty telemetry series for an orchard plot.
     *
     * @param plotId       the referenced plot identifier
     * @param sensorNodeId the optional bound IoT device identifier
     * @return a new valid {@link TelemetrySeries} instance
     */
    public static TelemetrySeries create(PlotId plotId, DeviceId sensorNodeId) {
        if (plotId == null) {
            throw new IllegalArgumentException("device.plot_id.null_or_empty");
        }
        return new TelemetrySeries(
                new TelemetrySeriesId(),
                plotId,
                sensorNodeId,
                TelemetrySeriesStatus.NORMAL,
                new ArrayList<>(),
                0L
        );
    }

    /**
     * Reconstitutes an aggregate instance from an immutable persistence snapshot.
     *
     * @param snapshot the aggregate snapshot
     * @return the reconstituted aggregate instance
     */
    public static TelemetrySeries reconstitute(TelemetrySeriesSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("telemetry.series.snapshot.null");
        }
        var reconstitutedReadings = snapshot.readings().stream()
                .map(HourlyTelemetryReading::reconstitute)
                .toList();

        return new TelemetrySeries(
                snapshot.id(),
                snapshot.plotId(),
                snapshot.sensorNodeId(),
                snapshot.currentStatus(),
                reconstitutedReadings,
                snapshot.revision()
        );
    }

    /**
     * Ingests a new hourly telemetry reading and updates operational status if stress thresholds are breached.
     *
     * @param reading the hourly telemetry reading to ingest
     */
    public void ingestReading(HourlyTelemetryReading reading) {
        if (reading == null) {
            throw new IllegalArgumentException("telemetry.reading.snapshot.null");
        }
        this.readings.add(reading);
        evaluateStatus(reading);
    }

    /**
     * Evaluates biological and meteorological stress thresholds.
     */
    private void evaluateStatus(HourlyTelemetryReading reading) {
        // Temperature below 0°C indicates frost risk
        if (reading.temperature().celsius() <= 0.0) {
            this.currentStatus = TelemetrySeriesStatus.FROST_ALERT;
        } else if (reading.soilMoisture().percentage() < 15.0) {
            // Soil moisture below 15% indicates severe hydric deficit
            this.currentStatus = TelemetrySeriesStatus.HYDRIC_STRESS_ACTIVE;
        } else {
            this.currentStatus = TelemetrySeriesStatus.NORMAL;
        }
    }

    /**
     * Exports an immutable snapshot of this aggregate root.
     *
     * @return an immutable {@link TelemetrySeriesSnapshot}
     */
    public TelemetrySeriesSnapshot snapshot() {
        var readingSnapshots = readings.stream()
                .map(HourlyTelemetryReading::snapshot)
                .toList();

        return new TelemetrySeriesSnapshot(
                id,
                plotId,
                sensorNodeId,
                currentStatus,
                readingSnapshots,
                revision
        );
    }

    public TelemetrySeriesId id() {
        return id;
    }

    public PlotId plotId() {
        return plotId;
    }

    public DeviceId sensorNodeId() {
        return sensorNodeId;
    }

    public TelemetrySeriesStatus currentStatus() {
        return currentStatus;
    }

    public List<HourlyTelemetryReading> readings() {
        return List.copyOf(readings);
    }

    public Long revision() {
        return revision;
    }
}

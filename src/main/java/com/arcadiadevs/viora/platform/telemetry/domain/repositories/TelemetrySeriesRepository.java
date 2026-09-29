package com.arcadiadevs.viora.platform.telemetry.domain.repositories;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.TelemetrySeries;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository port defining persistence operations for {@link TelemetrySeries} aggregates.
 */
public interface TelemetrySeriesRepository {

    /**
     * Finds the telemetry series aggregate associated with a monitored plot.
     *
     * @param plotId the plot identifier
     * @return an optional containing the aggregate if present
     */
    Optional<TelemetrySeries> findByPlotId(PlotId plotId);

    /**
     * Retrieves hourly telemetry reading snapshots for a plot, optionally bounded by date range.
     *
     * @param plotId    the plot identifier
     * @param startDate the optional inclusive start instant
     * @param endDate   the optional inclusive end instant
     * @return the list of matching reading snapshots in chronological order
     */
    List<HourlyTelemetryReadingSnapshot> findReadingsByPlotIdAndDateRange(PlotId plotId, Instant startDate, Instant endDate);

    /**
     * Persists or updates a telemetry series aggregate.
     *
     * @param series the aggregate root to persist
     * @return the saved aggregate instance
     */
    TelemetrySeries save(TelemetrySeries series);

    /**
     * Checks if a telemetry series exists for the given plot.
     *
     * @param plotId the plot identifier
     * @return true if exists; false otherwise
     */
    boolean existsByPlotId(PlotId plotId);
}

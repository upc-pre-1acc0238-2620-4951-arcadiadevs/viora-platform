package com.arcadiadevs.viora.platform.telemetry.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetTelemetrySeriesByPlotIdQuery;

import java.util.List;

/**
 * Application query service port orchestrating telemetry series retrieval use cases.
 */
public interface TelemetryQueryService {

    /**
     * Handles the retrieval of hourly telemetry readings for a plot within an optional date range.
     *
     * @param query the domain query containing query parameters
     * @return a Result containing the list of reading snapshots or an application error
     */
    Result<List<HourlyTelemetryReadingSnapshot>, ApplicationError> handle(GetTelemetrySeriesByPlotIdQuery query);
}

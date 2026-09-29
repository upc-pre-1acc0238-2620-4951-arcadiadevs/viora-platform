package com.arcadiadevs.viora.platform.telemetry.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetWeatherForecastByPlotIdQuery;

/**
 * Application query service port for querying 7-day agroclimatic weather forecasts.
 */
public interface WeatherForecastQueryService {

    /**
     * Handles the retrieval of 7-day weather forecast projections for a plot.
     *
     * @param query the query containing the plot identifier
     * @return Result containing the WeatherForecastSnapshot or an ApplicationError
     */
    Result<WeatherForecastSnapshot, ApplicationError> handle(GetWeatherForecastByPlotIdQuery query);
}

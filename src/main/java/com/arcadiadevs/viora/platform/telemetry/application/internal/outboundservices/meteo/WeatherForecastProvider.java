package com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDay;

import java.util.List;

/**
 * Outbound port interface for fetching 7-day meteorological forecast projections.
 */
public interface WeatherForecastProvider {

    /**
     * Retrieves the 7-day meteorological forecast for the specified geographical coordinates.
     *
     * @param latitude  the latitude in decimal degrees
     * @param longitude the longitude in decimal degrees
     * @return ordered list of 7 daily weather forecast entities
     */
    List<WeatherForecastDay> fetchSevenDayForecast(double latitude, double longitude);
}

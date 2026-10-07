package com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo;

import java.time.Instant;
import java.util.List;

/**
 * Outbound port for the hourly weather already observed at a location, used to feed the virtual node
 * of plots that have no physical sensor.
 */
public interface HourlyWeatherProvider {

    /**
     * Retrieves the hourly weather of the last days up to the current hour.
     *
     * @param latitude  the latitude in decimal degrees
     * @param longitude the longitude in decimal degrees
     * @param pastDays  how many days back to cover
     * @return the hourly observations in chronological order, or an empty list when the provider is unreachable
     */
    List<HourlyWeatherObservation> fetchPastHours(double latitude, double longitude, int pastDays);

    /**
     * Weather of one hour.
     *
     * @param hour                the start of the hour (UTC)
     * @param temperatureCelsius  air temperature at 2 m in °C
     * @param relativeHumidity    relative humidity at 2 m in %
     * @param soilMoisturePercent volumetric soil moisture of the layer that holds the 30 cm probe, in %
     * @param solarRadiation      shortwave solar radiation in W/m²
     */
    record HourlyWeatherObservation(
            Instant hour,
            double temperatureCelsius,
            double relativeHumidity,
            double soilMoisturePercent,
            double solarRadiation
    ) {
    }
}

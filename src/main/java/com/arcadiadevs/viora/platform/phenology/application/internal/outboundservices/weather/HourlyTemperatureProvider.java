package com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HourlyTemperature;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for the hourly air temperatures already observed at a location, used to count the winter
 * chill of a plot (US22, US23).
 */
public interface HourlyTemperatureProvider {

    /**
     * Local time of the plots; the hours returned and the "today" of the chill season are in this zone.
     */
    ZoneId PLOT_TIME_ZONE = ZoneId.of("America/Lima");

    /**
     * Retrieves the hourly temperatures of a range of days.
     *
     * @param latitude  the latitude in decimal degrees
     * @param longitude the longitude in decimal degrees
     * @param from      the first day, inclusive
     * @param to        the last day, inclusive
     * @return the hours that have a value, oldest first; empty when the provider cannot be reached
     */
    Optional<List<HourlyTemperature>> fetchHourlyTemperatures(double latitude, double longitude, LocalDate from, LocalDate to);
}

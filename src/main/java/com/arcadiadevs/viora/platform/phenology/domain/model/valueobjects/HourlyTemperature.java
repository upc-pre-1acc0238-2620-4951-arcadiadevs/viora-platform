package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

import java.time.LocalDateTime;

/**
 * Air temperature observed during one hour at the plot, in the plot's local time.
 *
 * @param hour    the start of the hour, local time of the plot
 * @param celsius the air temperature at 2 m, in °C
 */
public record HourlyTemperature(LocalDateTime hour, double celsius) {

    /**
     * Compact constructor validating the hour.
     */
    public HourlyTemperature {
        if (hour == null) {
            throw new IllegalArgumentException("phenology.hourly_temperature.hour.null");
        }
    }
}

package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * State of a chill season at the end of one day.
 *
 * @param date                  the local date
 * @param accumulatedPortions   the chill portions accumulated since June 1 up to the end of that day
 * @param maxTemperatureCelsius the highest hourly temperature of that day, in °C
 */
public record DailyChillPoint(LocalDate date, double accumulatedPortions, double maxTemperatureCelsius) {

    /**
     * Compact constructor validating the date.
     */
    public DailyChillPoint {
        if (date == null) {
            throw new IllegalArgumentException("phenology.daily_chill_point.date.null");
        }
    }
}

package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * The forecast minimum temperature of a day at a plot.
 *
 * @param date           the day, in the producer's time zone
 * @param minimumCelsius the forecast minimum
 */
public record DailyMinimum(LocalDate date, double minimumCelsius) {

    /**
     * Validates that the day is given.
     *
     * @throws IllegalArgumentException if the date is null
     */
    public DailyMinimum {
        if (date == null) {
            throw new IllegalArgumentException("territory.risk.forecast.date.null");
        }
    }
}

package com.arcadiadevs.viora.platform.phenology.interfaces.acl;

import java.time.LocalDate;
import java.util.SortedMap;

/**
 * ACL facade exposing Phenology bounded context capabilities to external contexts.
 *
 * <p>Only primitive, published-language structures cross the boundary; Phenology aggregates and value
 * objects stay internal.</p>
 */
public interface PhenologyContextFacade {

    /**
     * Retrieves the historical harvest yields registered by the producer for a plot (US20 history).
     *
     * @param plotId the plot identifier string (UUID)
     * @return total kilograms per campaign year in ascending year order; empty when the plot has no history,
     *         does not exist or is not active
     */
    SortedMap<Integer, Double> findHistoricalYields(String plotId);

    /**
     * Retrieves the daily maximum temperature at a plot, from the same hourly weather the chill model reads.
     *
     * @param plotId the plot identifier string (UUID)
     * @param from   first day, inclusive
     * @param to     last day, inclusive
     * @return maximum in degrees Celsius per local day (America/Lima), ascending; empty without location or weather
     */
    SortedMap<LocalDate, Double> findDailyMaxTemperatures(String plotId, LocalDate from, LocalDate to);
}

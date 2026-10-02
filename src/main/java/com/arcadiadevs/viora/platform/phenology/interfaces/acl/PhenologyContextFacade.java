package com.arcadiadevs.viora.platform.phenology.interfaces.acl;

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
}

package com.arcadiadevs.viora.platform.orchard.interfaces.acl;

import java.util.Optional;

/**
 * ACL facade exposing Orchard bounded context capabilities to external contexts.
 *
 * <p>Provides a clean integration surface without leaking Orchard domain aggregates,
 * internal value objects, or persistence entities to consuming bounded contexts.</p>
 */
public interface OrchardContextFacade {

    /**
     * Checks if a plot exists and is currently in ACTIVE operational status.
     *
     * @param plotId the plot identifier string (UUID)
     * @return {@code true} if the plot exists and its status is ACTIVE; {@code false} otherwise
     */
    boolean existsActivePlot(String plotId);

    /**
     * Retrieves the calculated centroid coordinates [latitude, longitude] for an active plot.
     *
     * @param plotId the plot identifier string (UUID)
     * @return Optional containing double array [latitude, longitude], or empty if plot is not found or not active
     */
    Optional<double[]> findPlotCentroid(String plotId);
}

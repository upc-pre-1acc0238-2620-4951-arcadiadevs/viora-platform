package com.arcadiadevs.viora.platform.orchard.interfaces.acl;

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
}

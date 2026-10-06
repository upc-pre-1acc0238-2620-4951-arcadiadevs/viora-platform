package com.arcadiadevs.viora.platform.thinning.interfaces.acl;

import java.util.Optional;

/**
 * ACL facade exposing Thinning bounded context capabilities to external contexts.
 *
 * <p>Provides a clean integration surface without leaking Thinning domain aggregates, internal value objects, or
 * persistence entities to consuming bounded contexts.</p>
 */
public interface ThinningContextFacade {

    /**
     * Retrieves the commercial size grade of a caliber expressed as number of fruits per kilogram.
     *
     * <p>The grade is the label of the International Olive Council size scale the caliber falls into, for example
     * {@code "91/100"}.</p>
     *
     * @param fruitsPerKg number of fruits per kilogram; may be null when the caliber is unknown
     * @return Optional containing the grade label, or empty if the caliber is absent
     */
    Optional<String> findCommercialSizeGrade(Double fruitsPerKg);
}

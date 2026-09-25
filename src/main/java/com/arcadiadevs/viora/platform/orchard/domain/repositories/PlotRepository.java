package com.arcadiadevs.viora.platform.orchard.domain.repositories;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotName;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;

import java.util.Optional;

/**
 * Domain repository port for managing the persistence lifecycle of {@link Plot} aggregate roots.
 */
public interface PlotRepository {

    /**
     * Saves a plot aggregate root.
     *
     * @param plot the plot aggregate to persist
     * @return the saved plot aggregate root
     */
    Plot save(Plot plot);

    /**
     * Finds a plot by its unique domain identifier.
     *
     * @param id the plot identifier
     * @return an Optional containing the plot if found, or empty otherwise
     */
    Optional<Plot> findById(PlotId id);

    /**
     * Checks if an active plot with the given name already exists for the producer.
     *
     * @param name       the plot name to check
     * @param producerId the producer identifier value object
     * @return true if a plot with the name exists for the producer, false otherwise
     */
    boolean existsByNameAndProducerId(PlotName name, ProducerId producerId);
}

package com.arcadiadevs.viora.platform.phenology.domain.repositories;

import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.TrackerId;

import java.util.Optional;

/**
 * Domain repository port for managing the lifecycle of {@link ChillAccumulationTracker} aggregate roots.
 * Pure Java interface decoupled from persistence frameworks.
 */
public interface ChillAccumulationTrackerRepository {

    /**
     * Persists the tracker aggregate root and its subordinated harvest entries.
     *
     * @param tracker the aggregate root to save
     * @return the saved aggregate root instance
     */
    ChillAccumulationTracker save(ChillAccumulationTracker tracker);

    /**
     * Finds a tracker by its unique domain identifier.
     *
     * @param id the tracker identifier
     * @return an {@link Optional} containing the tracker if found
     */
    Optional<ChillAccumulationTracker> findById(TrackerId id);

    /**
     * Finds the tracker associated with a given orchard plot.
     *
     * @param plotId the plot identifier
     * @return an {@link Optional} containing the tracker if found
     */
    Optional<ChillAccumulationTracker> findByPlotId(PlotId plotId);

    /**
     * Checks if a harvest record already exists for a specific plot and campaign year.
     *
     * @param plotId the plot identifier
     * @param year   the campaign year
     * @return true if an entry exists, false otherwise
     */
    boolean existsByPlotIdAndCampaignYear(PlotId plotId, CampaignYear year);
}

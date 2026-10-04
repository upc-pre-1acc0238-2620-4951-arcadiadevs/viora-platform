package com.arcadiadevs.viora.platform.telemetry.domain.repositories;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentId;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentStatus;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository port defining persistence and retrieval operations for {@link AgroclimaticIncident} aggregates.
 *
 * <p>Pure Java interface without Spring Data or JPA dependencies.</p>
 */
public interface AgroclimaticIncidentRepository {

    /**
     * Persists or updates an incident aggregate root.
     *
     * @param incident the aggregate root to persist
     * @return the saved aggregate
     */
    AgroclimaticIncident save(AgroclimaticIncident incident);

    /**
     * Finds an incident by its unique identifier.
     *
     * @param id the incident identifier
     * @return Optional containing the aggregate root if found
     */
    Optional<AgroclimaticIncident> findById(IncidentId id);

    /**
     * Retrieves all incidents recorded for a given plot.
     *
     * @param plotId the plot identifier
     * @return list of matching incidents
     */
    List<AgroclimaticIncident> findByPlotId(PlotId plotId);

    /**
     * Retrieves incidents for a plot with a specific operational status.
     *
     * @param plotId the plot identifier
     * @param status the operational status
     * @return list of matching incidents
     */
    List<AgroclimaticIncident> findByPlotIdAndStatus(PlotId plotId, IncidentStatus status);

    /**
     * Searches incidents matching optional filters.
     *
     * @param plotId   optional plot filter
     * @param status   optional status filter
     * @param severity optional severity filter
     * @return list of matching incidents
     */
    List<AgroclimaticIncident> findAll(PlotId plotId, IncidentStatus status, IncidentSeverity severity);

    /**
     * Checks if an active incident of the given type already exists for a plot.
     *
     * @param plotId the plot identifier
     * @param type   the incident type
     * @return true if an active incident exists, false otherwise
     */
    boolean existsActiveByPlotIdAndType(PlotId plotId, IncidentType type);

    /**
     * Finds the active incident of a specific type for a plot, if any.
     *
     * @param plotId the plot identifier
     * @param type   the incident type
     * @return Optional containing the active aggregate root if found
     */
    Optional<AgroclimaticIncident> findActiveByPlotIdAndType(PlotId plotId, IncidentType type);
}

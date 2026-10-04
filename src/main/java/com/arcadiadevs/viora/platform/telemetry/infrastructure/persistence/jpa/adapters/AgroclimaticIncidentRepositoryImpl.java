package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentId;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentStatus;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.assemblers.AgroclimaticIncidentPersistenceAssembler;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities.AgroclimaticIncidentPersistenceEntity;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.repositories.AgroclimaticIncidentPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter bridging the domain {@link AgroclimaticIncidentRepository} port with Spring Data JPA.
 */
@Repository
public class AgroclimaticIncidentRepositoryImpl implements AgroclimaticIncidentRepository {

    private final AgroclimaticIncidentPersistenceRepository persistenceRepository;

    /**
     * Constructs the adapter injecting the Spring Data persistence repository.
     *
     * @param persistenceRepository the underlying Spring Data JPA repository
     */
    public AgroclimaticIncidentRepositoryImpl(AgroclimaticIncidentPersistenceRepository persistenceRepository) {
        if (persistenceRepository == null) {
            throw new IllegalArgumentException("incident.repository.null");
        }
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public AgroclimaticIncident save(AgroclimaticIncident incident) {
        if (incident == null) {
            throw new IllegalArgumentException("incident.snapshot.null");
        }
        var uuid = UUID.fromString(incident.snapshot().id().incidentId());
        var existingOpt = persistenceRepository.findById(uuid);

        AgroclimaticIncidentPersistenceEntity entityToSave;
        if (existingOpt.isPresent()) {
            entityToSave = AgroclimaticIncidentPersistenceAssembler.updateEntityFromDomain(existingOpt.get(), incident);
        } else {
            entityToSave = AgroclimaticIncidentPersistenceAssembler.toPersistenceFromDomain(incident);
        }

        var savedEntity = persistenceRepository.save(entityToSave);
        return AgroclimaticIncidentPersistenceAssembler.toDomainFromPersistence(savedEntity);
    }

    @Override
    public Optional<AgroclimaticIncident> findById(IncidentId id) {
        if (id == null) {
            return Optional.empty();
        }
        return persistenceRepository.findById(UUID.fromString(id.incidentId()))
                .map(AgroclimaticIncidentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<AgroclimaticIncident> findByPlotId(PlotId plotId) {
        if (plotId == null) {
            return List.of();
        }
        return persistenceRepository.findByPlotIdOrderByTriggeredAtDesc(plotId).stream()
                .map(AgroclimaticIncidentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<AgroclimaticIncident> findByPlotIdAndStatus(PlotId plotId, IncidentStatus status) {
        if (plotId == null || status == null) {
            return List.of();
        }
        return persistenceRepository.findByPlotIdAndStatusOrderByTriggeredAtDesc(plotId, status).stream()
                .map(AgroclimaticIncidentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<AgroclimaticIncident> findAll(PlotId plotId, IncidentStatus status, IncidentSeverity severity) {
        return persistenceRepository.findByCriteria(plotId, status, severity).stream()
                .map(AgroclimaticIncidentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public boolean existsActiveByPlotIdAndType(PlotId plotId, IncidentType type) {
        if (plotId == null || type == null) {
            return false;
        }
        return persistenceRepository.findFirstByPlotIdAndTypeAndStatus(plotId, type, IncidentStatus.ACTIVE).isPresent();
    }

    @Override
    public Optional<AgroclimaticIncident> findActiveByPlotIdAndType(PlotId plotId, IncidentType type) {
        if (plotId == null || type == null) {
            return Optional.empty();
        }
        return persistenceRepository.findFirstByPlotIdAndTypeAndStatus(plotId, type, IncidentStatus.ACTIVE)
                .map(AgroclimaticIncidentPersistenceAssembler::toDomainFromPersistence);
    }
}

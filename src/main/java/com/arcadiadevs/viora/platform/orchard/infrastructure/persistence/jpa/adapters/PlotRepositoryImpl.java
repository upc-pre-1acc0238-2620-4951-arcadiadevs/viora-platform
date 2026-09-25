package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotName;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotStatus;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.assemblers.PlotPersistenceAssembler;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.repositories.PlotPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter that bridges the domain {@link PlotRepository} port with Spring Data JPA.
 */
@Repository
public class PlotRepositoryImpl implements PlotRepository {

    private final PlotPersistenceRepository plotPersistenceRepository;

    /**
     * Constructs the adapter injecting the persistence repository.
     *
     * @param plotPersistenceRepository the underlying Spring Data JPA persistence repository
     */
    public PlotRepositoryImpl(PlotPersistenceRepository plotPersistenceRepository) {
        this.plotPersistenceRepository = plotPersistenceRepository;
    }

    @Override
    public Optional<Plot> findById(PlotId id) {
        return plotPersistenceRepository.findById(UUID.fromString(id.plotId()))
                .map(PlotPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Plot save(Plot plot) {
        var savedEntity = plotPersistenceRepository.save(PlotPersistenceAssembler.toPersistenceFromDomain(plot));
        return PlotPersistenceAssembler.toDomainFromPersistence(savedEntity);
    }

    @Override
    public boolean existsByNameAndProducerId(PlotName name, ProducerId producerId) {
        return plotPersistenceRepository.existsByNameAndProducerId(name, producerId);
    }

    @Override
    public List<Plot> findActiveByProducerId(ProducerId producerId) {
        return plotPersistenceRepository.findAllByProducerIdAndStatus(producerId, PlotStatus.ACTIVE)
                .stream()
                .map(PlotPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<Plot> findByProducerIdAndUpdatedSince(ProducerId producerId, Instant updatedSince) {
        return plotPersistenceRepository.findAllByProducerIdAndUpdatedAtGreaterThanEqual(producerId, updatedSince)
                .stream()
                .map(PlotPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }
}

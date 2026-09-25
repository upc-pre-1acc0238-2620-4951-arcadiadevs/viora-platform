package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotName;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotStatus;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.entities.PlotPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA persistence repository for {@link PlotPersistenceEntity}.
 */
@Repository
public interface PlotPersistenceRepository extends JpaRepository<PlotPersistenceEntity, UUID> {

    /**
     * Checks if a plot exists with the specified name and producer identifier.
     *
     * @param name       the plot name value object
     * @param producerId the producer identifier value object
     * @return true if a matching record exists, false otherwise
     */
    boolean existsByNameAndProducerId(PlotName name, ProducerId producerId);

    /**
     * Retrieves all plots belonging to a producer with matching status.
     *
     * @param producerId the producer identifier value object
     * @param status     the plot status
     * @return list of matching plot persistence entities
     */
    List<PlotPersistenceEntity> findAllByProducerIdAndStatus(ProducerId producerId, PlotStatus status);

    /**
     * Retrieves plots belonging to a producer updated at or after the given instant.
     *
     * @param producerId   the producer identifier value object
     * @param updatedSince the timestamp lower bound
     * @return list of matching plot persistence entities
     */
    List<PlotPersistenceEntity> findAllByProducerIdAndUpdatedAtGreaterThanEqual(ProducerId producerId, Instant updatedSince);
}

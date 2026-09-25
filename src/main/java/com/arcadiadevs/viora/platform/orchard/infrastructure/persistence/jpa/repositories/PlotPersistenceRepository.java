package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotName;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.entities.PlotPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

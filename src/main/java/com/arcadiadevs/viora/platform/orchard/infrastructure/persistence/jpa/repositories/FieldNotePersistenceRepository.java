package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.entities.FieldNotePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FieldNotePersistenceRepository extends JpaRepository<FieldNotePersistenceEntity, UUID> {

    List<FieldNotePersistenceEntity> findByPlotIdOrderByObservedOnDescRecordedAtDesc(String plotId);

    /** Everything of a deleted account, for the account purge. */
    List<FieldNotePersistenceEntity> findAllByProducerId(String producerId);
}

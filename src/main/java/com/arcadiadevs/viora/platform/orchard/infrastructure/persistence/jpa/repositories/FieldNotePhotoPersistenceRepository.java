package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.entities.FieldNotePhotoPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FieldNotePhotoPersistenceRepository extends JpaRepository<FieldNotePhotoPersistenceEntity, UUID> {
}

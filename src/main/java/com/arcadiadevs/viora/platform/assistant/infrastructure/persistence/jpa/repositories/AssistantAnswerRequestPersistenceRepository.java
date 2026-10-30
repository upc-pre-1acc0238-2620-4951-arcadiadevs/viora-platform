package com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.entities.AssistantAnswerRequestPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssistantAnswerRequestPersistenceRepository
        extends JpaRepository<AssistantAnswerRequestPersistenceEntity, UUID> {

    Optional<AssistantAnswerRequestPersistenceEntity> findByProducerIdAndClientRequestId(String producerId,
            UUID clientRequestId);

    /** Every key of a deleted account, for the account purge. */
    List<AssistantAnswerRequestPersistenceEntity> findAllByProducerId(String producerId);
}
